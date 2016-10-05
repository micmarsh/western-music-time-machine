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

(deftest test-play
  (testing "Nothing Enqueued"
    (let [player (get-in @sample-data ui/player-path)]
      (is (= {:db player} (ui/player-play player)))))
  (testing "With Enqueued"
    (let [tracks (into [] (take 5) @sample-tracks)
          with-tracklist (assoc-in @sample-data (conj ui/player-path :player/track-list) tracks)
          with-enqueued (update-in with-tracklist ui/player-path
                                   #(reduce (fn [player track-id]
                                              (->> track-id
                                                   (ui/player-track-lookup player)
                                                   (ui/player-enqueue-track player)))
                                            % (map :track/id tracks))
                                   ;; TODO wew lad, can do better than
                                   ;; that^. Prolly a little refactor
                                   ;; action in the real code, too
                                   ;; (then can test!)
                                   )
          player (get-in with-enqueued ui/player-path)
          with-playing (ui/player-play player)]
      (is (= (first tracks) (:player/playing (:db with-playing))))
      (is (= [:new-track-playing (first tracks) false]
             (:dispatch with-playing))))))
