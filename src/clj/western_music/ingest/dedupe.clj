(ns western-music.ingest.dedupe)

(defn find-alias [conversions name]
  (->> conversions
       (filter (comp #(contains? % name) :aliases))
       (first)))

(defn update-value [conversions path map]
  (update-in map path
             (fn [value]
               (if-let [alias (find-alias conversions value)]
                 (:global-name alias)
                 value))))

(comment
  (require '[western-music.data :refer [initial-data]]
           '[western-music.data.nations :as nations]
           '[western-music.data.composers :as composers])

  (def fixed-nations
    (map (partial update-value nations/name-conversion
                  [:western-music.spec/composer :composer/birth :place/nation])
         initial-data))

  (def fixed-composers
    (map (partial update-value composers/name-conversion
                  [:western-music.spec/composer :composer/name])
         fixed-nations))

  (def all-fixed
    (map (partial update-value composers/name-conversion
                  [:composition/tracks 0 :track/artist])
         fixed-composers))
  
  )
