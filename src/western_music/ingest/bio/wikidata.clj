(ns western-music.ingest.bio.wikidata
  (:require [clj-http.lite.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]))

(declare lookup-nation lookup-city)

(def ^:const base-url "https://www.wikidata.org/w/api.php")

(def ^:const property-keys
  {:place-of-birth :P19
   :date-of-birth :P569
   :title :P373 ;; technically "Commons Category". Regardless, seems to be a
   ;; good way to cut to the chase and get an identifier
   })

(defn property
  ([key properties]
     (property key properties nil))
  ([key properties default]
     (get properties
          (or (get property-keys key)
              (throw (ex-info "Property key doesn't exist"
                              {:key key
                               :properties property
                               :property-keys property-keys})))
          default)))

(def id-print #(do (println %) %))

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

(defn id-search
  "Given a 'title' (a composer in current cases), return
   the wikidata entity id (example \"Q9695\")"
  [title]
  (->> {:action "wbsearchentities" :search title}
       (base-query)
       (body)
       (search-id-value)))

(defn properties
  "Given a wikidata id, return the map of associated properties
   TODO: figure out why promise is super broken, just use callback for now"
  [id]
  (->> {:action "wbgetclaims" :entity id}
       (base-query)
       (body)
       (:claims)))

(defmulti prop-value (comp :datatype :mainsnak))

(defn year [string-time]
  (apply str (take 4 (rest string-time))))

(def value (comp :value :datavalue :mainsnak))

(defmethod prop-value "string"
  [prop]
  (value prop))

(defmethod prop-value "time"
  [prop]
  (-> prop
      (value)
      (:time)
      (year)))

(defmethod prop-value "wikibase-item"
  [prop]
  (let [id (str "Q" (:numeric-id (value prop)))]
    (properties id)))

(defn lookup-year [data]
  (let [who (:name (:composer data))]
    (->> (id-search who)
         (properties)
         (property :date-of-birth)
         (first)
         (prop-value))))
