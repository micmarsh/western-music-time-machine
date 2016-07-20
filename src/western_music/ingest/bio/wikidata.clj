(ns western-music.ingest.bio.wikidata
  (:require [clj-http.lite.client :as http]
            [western-music.util :refer [defcached]]
            [clj-time
             [core :as t]
             [format :as f]]
            [clojure.string :as str]
            [cheshire.core :as json]))

;; HTTP API interaction
(def ^:const base-url "https://www.wikidata.org/w/api.php")

(defn base-query
  "GET query to wikidata with the given parameters"
  [params]
  (http/get base-url
            {:query-params (merge {:language "en"
                                   :format "json"}
                                  params)}))

(defn body [http-response]
  (-> http-response (:body) (json/decode true)))

;; "Querying", still basically HTTP stuff
(defn search-id-value
  "Get the id \"Q-whatever\" from a wikidata response
   TODO extend w/ id-search specific error handling/conflict resolution"
  [body]
  (let [results (:search body)]
    (if (empty? results)
      (throw (ex-info "Empty search response" {::search body}))
      (-> results first :id))))

(def ^:const translations
  {"Antonín Dvorak" "Antonín Dvořák"
   "Jean-Baptista Lully" "Jean-Baptiste Lully"
   "Ralph Vaughn Williams" "Ralph Vaughan Williams"
   "Pyotr Il'yich Tchaikovsky" "Pyotr Ilyich Tchaikovsky"
   "Sergey Prokofiev" "Sergei Prokofiev"
   "Sergei Rachmaninov" "Sergei Rachmaninoff"
   "Nikolay Andreyevich Rimsky-Korsakov" "Nikolai Rimsky-Korsakov"
   "Alexander Porfir'yevich Borodin" "Alexander Borodin"
   "Cesar Franck" "César Franck"
   "John Milford Rutter" "John Rutter"
   "Antonio Martin Y Col" "Antonio Martín y Coll"
   "Alonso de Mudarra" "Alonso Mudarra"
   "Joao Domingos Bomtempo" "João Domingos Bomtempo"})

(defn translate [search-term]
  (get translations search-term search-term))

(defcached id-search
  "Given a 'title' (a composer in current cases), return
   the wikidata entity id (example \"Q9695\")"
  [title]
  (println "searchin for" title)
  (let [search-term (translate title)
        response (base-query {:action "wbsearchentities" :search search-term})]
    (try
      (search-id-value (body response))
      (catch clojure.lang.ExceptionInfo e
        (if (contains? (ex-data e) ::search)
          (throw (ex-info (str "Bad search results for " title)
                          {:search-term title
                           :http-response response}))
          (throw e))))))

(defcached properties-query
  "Given a wikidata id, return the map of associated properties"
  [id]
  (println "yooo id" id)
  (->> {:action "wbgetclaims" :entity id}
       (base-query)
       (body)
       (:claims)))

;; Reading/Coercing/Re-reading properties from query results
(def ^:const property-keys
  {:place-of-birth :P19
   :date-of-birth :P569
   :country :P17
   :title :P373})

(defmulti coerce-property (comp :datatype :mainsnak))

(defn property-key
  [key properties]
  (or (get properties
           (or (get property-keys key)
               (throw (ex-info "Property key doesn't exist"
                               {:key key
                                :properties properties
                                :property-keys property-keys}))))
      (throw (ex-info (str "Couldn't find value " key)
                      {:key key
                       :translated-key (get property-keys key)
                       :properties properties}))))

(defn property
  [key properties]
  (-> (property-key key properties)
      (first)
      (coerce-property)))

(def value (comp :value :datavalue :mainsnak))

(defmethod coerce-property "string"
  [prop]
  (value prop))

(defmethod coerce-property "time"
  [prop]
  (->> prop
      (value)
      (:time)
      (f/parse (f/formatters :date-time-no-ms))))

(defmethod coerce-property "wikibase-item"
  [prop]
  (let [id (str "Q" (:numeric-id (value prop)))]
    (properties-query id)))

(defmethod coerce-property :default
  [prop]
  (throw (ex-info "Encountered value of unknown type"
                  {:raw-prop prop
                   :value (value prop)})))

;; Functions exposed to rest of application
(defn lookup-year [data]
  (let [who (:name (:composer data))]
    (->> (id-search who)
         (properties-query)
         (property :date-of-birth)
         (t/year))))

(defn lookup-city [data]
  (let [who (:name (:composer data))]
    (->> (id-search who)
         (properties-query)
         (property :place-of-birth)
         (property :title))))

(defn lookup-nation [data]
  (let [who (:name (:composer data))]
    (->> (id-search who)
         (properties-query)
         (property :place-of-birth)
         (property :country)
         (property :title))))

(comment
  (lookup-year {:composer {:name "Franz Liszt"}})
  )
