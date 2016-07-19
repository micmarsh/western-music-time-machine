(ns western-music.ingest.bio.wikidata
  (:require [org.httpkit.client :as http]
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

(defmacro defasync [name doc args & body]
  `(defn ~name ~doc
     ([~@(drop-last args)]
        (let [p# (promise)]
          (~name ~@(drop-last args) (fn [val#] (p# val#) val#))
          p#))
     ([~@args] ~@body)))

(def id-print #(do (println %) %))

(defasync base-query
  "GET query to wikidata with the given parameters"
  [params callback]
  (http/get base-url {:query-params (merge {:language "en"
                                            :format "json"}
                                           params)}
            callback))

(defn body [http-response]
  (-> http-response (:body) (json/decode true)))

(def search-id-value
  "Get the id \"Q-whatever\" from a wikidata response
   TODO extend w/ id-search specific error handling/conflict resolution"
  (comp :id first :search))

(defasync id-search
  "Given a 'title' (a composer in current cases), return
   the wikidata entity id (example \"Q9695\")"
  [title callback]
  (base-query {:action "wbsearchentities" :search title}
              (comp callback search-id-value body)))

(defasync properties
  "Given a wikidata id, return the map of associated properties
   TODO: figure out why promise is super broken, just use callback for now"
  [id callback]
  (base-query {:action "wbgetclaims" :entity id}
              (comp callback :claims body)))

(defmulti prop-value* (fn [prop _] (:datatype (:mainsnak prop))))

(defn year [string-time]
  (apply str (take 4 (rest string-time))))

(def value (comp :value :datavalue :mainsnak))

(defmethod prop-value* "string"
  [prop callback]
  (callback (value prop)))

(defmethod prop-value* "time"
  [prop callback]
  (-> prop
      (value)
      (:time)
      (year)
      (callback)))

(defmethod prop-value* "wikibase-item"
  [prop callback]
  (let [id (str "Q" (:numeric-id (value prop)))]
    (properties id callback)))

(defasync prop-value
  "Asynchronous, pull proper \"value\" representation out of a given
   item. Extend with prop-value*"
  [p c]
  (prop-value* p c))


#_(defn lookup-year [data]
  (let [who (:name (:composer data))]
    (->> (id-search who)
         (properties)
         (property :date-of-birth)
         (first)
         (prop-value))))

(defn lookup-year [data]
  (let [who (:name (:composer data))
        p (promise)]
    (id-search who
               (fn [id]
                 (properties id
                             (comp #(prop-value % (partial deliver p)) first (partial property :date-of-birth)))))
    p))


;; TODO hey, that shit was fuckin easy, now what to do about place?
;; * from Liszt "Raiding", got the idea that "Commons Category" for
;; place is city, another query 
