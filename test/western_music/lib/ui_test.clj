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

(deftest enqueue-track-order
  (testing "player-enqueue-track enqueues tracks in expected order"
    (let [player (get-in @sample-data ui/player-path)
          queue (:player/queue (reduce ui/player-enqueue-track player @sample-tracks))]
      (is (= (map :track/id @sample-tracks)
             (map :track/id queue))))))

(deftest test-player-controls
  (testing "Nothing Enqueued"
    (let [player (get-in @sample-data ui/player-path)]
      (is (= {:db player} (ui/player-play player)))))
  
  (testing "With Enqueued"
    (let [tracks (take 5 @sample-tracks)
          with-enqueued (enqueue-tracks-directly @sample-data tracks)
          player (get-in with-enqueued ui/player-path)
          playing-fx (ui/player-play player)]
      (is (= (first tracks) (:player/playing (:db playing-fx))))
      (is (false? (:player/paused (:db playing-fx))))
      (is (= [:new-track-playing (first tracks) false]
             (:dispatch playing-fx)))

      (testing "Pause, then play again"
        (let [paused-fx (ui/player-pause (:db playing-fx))
              playing-fx (ui/player-play (:db paused-fx))]
          (is (= [:current-track-paused] (:dispatch paused-fx)))
          (is (true? (:player/paused (:db paused-fx))))
          
          (is (= [:current-track-playing] (:dispatch playing-fx)))
          (is (false? (:player/paused (:db playing-fx))))
          (is (= (first tracks) (:player/playing (:db playing-fx)))
              "Currently playing didn't change at all")))

      (testing "Forward and Backwards"
        (testing "forward while playing"
          (let [forwards (rest (reductions (fn [{player :db} _] (ui/player-forward player))
                                           playing-fx
                                           (:player/queue (:db playing-fx))))]
            (doseq [[expected-track fx index] (map vector (rest tracks) (drop-last forwards) (range))]
              (testing (str "call number " (inc index))
                (is (= expected-track (:player/playing (:db fx))))
                (is (false? (:player/paused (:db fx))))
                (is (= :queue (:player/selected-tab (:db fx))))
                (is (= [:new-track-playing expected-track false] (:dispatch fx)))))
            (is (= 1 (count (last forwards)))
                "No extra effects on last call")
            (is (= (:db (last forwards)) (:db (last (drop-last forwards))))
                "Forwarding when on last track doesn't change player state")))
        
        (testing "forward while paused"
          (let [paused-fx (ui/player-pause (:db playing-fx))
                forwards (rest (reductions (fn [{player :db} _] (ui/player-forward player))
                                           paused-fx
                                           (:player/queue (:db paused-fx))))]
            (doseq [[expected-track fx index] (map vector (rest tracks) (drop-last forwards) (range))]
              (testing (str "call number " (inc index))
                (is (= expected-track (:player/playing (:db fx))))
                (is (true? (:player/paused (:db fx))))
                (is (= :queue (:player/selected-tab (:db fx))))
                (is (= [:new-track-playing expected-track true] (:dispatch fx)))))
            (is (= 1 (count (last forwards)))
                "No extra effects on last call")
            (is (= (:db (last forwards)) (:db (last (drop-last forwards))))
                "Forwarding when on last track doesn't change player state")))))))
