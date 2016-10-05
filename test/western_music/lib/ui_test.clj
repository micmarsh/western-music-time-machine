(ns western-music.lib.ui-test
  (:refer-clojure :exclude [read-string])
  (:require [western-music.lib.composition :as comp]
            [clojure.test :refer [deftest is testing run-tests]]
            [western-music.lib.ui :as ui]
            [clojure.edn :refer [read-string]]))

(def sample-data
  (->> "test/sample.edn"
       (slurp)
       (read-string)
       (assoc ui/initial-data :data/raw)
       (delay)))

(def sample-tracks
  (delay (map comp/track (:data/raw @sample-data))))

(defn enqueue-tracks-directly [data tracks]
  (update-in data ui/player-path #(reduce ui/player-enqueue-track % tracks)))

(deftest test-play
  (testing "Nothing Enqueued"
    (let [player (get-in @sample-data ui/player-path)]
      (is (= {:db player} (ui/player-play player)))))
  (testing "With Enqueued"
    (let [tracks (into [] (take 5) @sample-tracks)
          with-enqueued (enqueue-tracks-directly @sample-data tracks)
          player (get-in with-enqueued ui/player-path)
          with-playing (ui/player-play player)]
      (is (= (first tracks) (:player/playing (:db with-playing))))
      (is (= [:new-track-playing (first tracks) false]
             (:dispatch with-playing))))))
