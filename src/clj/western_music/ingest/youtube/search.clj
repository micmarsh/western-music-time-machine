(ns western-music.ingest.youtube.search
  (:require [clj-http.lite.client :as http]
            [clojure.spec :as s]
            [western-music.lib.track]
            [cheshire.core :as json]))

(def ^:const search-url
  "https://content.googleapis.com/youtube/v3/search")

(defn search [api-key query]
  (http/get search-url
            {:query-params {:key api-key
                            :q query
                            :part "id,snippet"}}))
(defn body [http-response]
  (-> http-response (:body) (json/decode true)))

(def id (comp :videoId :id first :items))

(defn search-title [t]
  (str (:track/artist t) " " (:track/title t)))

(def new-ids (atom 0))

(defn youtube-track [api-key track]
  (let [search-term (search-title track)]
    (-> track
        (assoc :track/type :track/youtube)
        (assoc :track/youtube-id (id (body (search api-key search-term))))
        (assoc :track/id (+ 200 (swap! new-ids inc))))))

(defn verify [spec item]
  (if (s/valid? spec item)
    item
    (throw (ex-info (str "spec check failed: " (s/explain-str spec item))
                    (s/explain-data spec item)))))

(comment
  (require '[western-music.data :refer [initial-data]]
           '[western-music.lib.composition :as comp])

  (def with-youtube
    (map (fn [comp]
           (verify :western-music.spec/composition
                   (comp/add-track comp (youtube-track API_KEY (comp/track comp)))))
         initial-data))
  
  )
