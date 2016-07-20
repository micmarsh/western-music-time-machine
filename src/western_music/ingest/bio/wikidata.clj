(ns western-music.ingest.bio.wikidata
  (:require [clj-http.lite.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]))

(def ^:const base-url "https://www.wikidata.org/w/api.php")

(def ^:const property-keys
  {:place-of-birth :P19
   :date-of-birth :P569
   :country :P17
   :title :P373})

(defn base-query
  "GET query to wikidata with the given parameters"
  [params]
  (http/get base-url
            {:query-params (merge {:language "en"
                                   :format "json"}
                                  params)}))

(defn body [http-response]
  (-> http-response (:body) (json/decode true)))

(def search-id-value
  "Get the id \"Q-whatever\" from a wikidata response
   TODO extend w/ id-search specific error handling/conflict resolution"
  (comp :id first :search))

(defmacro defcached
  "Define a memoized + syncronized, single argument function"
  [name doc args & body]
  `(let [memoized# (memoize (fn [~(first args)] ~@body))]
     (defn ~name [arg#] (locking arg# (memoized# arg#)))))

(defcached id-search
  "Given a 'title' (a composer in current cases), return
   the wikidata entity id (example \"Q9695\")"
  [title]
  (->> {:action "wbsearchentities" :search title}
       (base-query)
       (body)
       (search-id-value)))

(defcached properties
  "Given a wikidata id, return the map of associated properties"
  [id]
  (->> {:action "wbgetclaims" :entity id}
       (base-query)
       (body)
       (:claims)))

(defmulti prop-value (comp :datatype :mainsnak))

(defn property-key
  [key properties]
  (get properties
       (or (get property-keys key)
           (throw (ex-info "Property key doesn't exist"
                           {:key key
                            :properties properties
                            :property-keys property-keys})))))

(defn property
  [key properties]
  (-> (property-key key properties)
      (first)
      (prop-value)))


(defn year [string-time]
  (apply str (take 4 (rest string-time))))

(def value (comp :value :datavalue :mainsnak))

(defmethod prop-value "string"
  [prop]
  (value prop))

(defmethod prop-value "time"
  ;; TODO this should actually return a time that something else can
  ;; pull a year out of 
  [prop]
  (-> prop
      (value)
      (:time)
      (year)))

(defmethod prop-value "wikibase-item"
  [prop]
  (let [id (str "Q" (:numeric-id (value prop)))]
    (properties id)))

(defmethod prop-value :default
  [prop]
  (throw (ex-info "Encountered value of unknown type"
                  {:raw-prop prop
                   :value (value prop)})))

(defn lookup-year [data]
  (let [who (:name (:composer data))]
    (->> (id-search who)
         (properties)
         (property :date-of-birth))))

(defn lookup-city [data]
  (let [who (:name (:composer data))]
    (->> (id-search who)
         (properties)
         (property :place-of-birth)
         (property :title))))

(defn lookup-nation [data]
  (let [who (:name (:composer data))]
    (->> (id-search who)
         (properties)
         (property :place-of-birth)
         (property :country)
         (property :title))))
