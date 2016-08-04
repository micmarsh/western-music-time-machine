(ns western-music.ingest.youtube.search
  (:require [clj-http.lite.client :as http]
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

(defn youtube-track [api-key track]
  (let [search-term (search-title track)]
    (-> track
        (assoc :track/type :track/youtube)
        (assoc :track/youtube-id (id (body (search api-key search-term)))))))
