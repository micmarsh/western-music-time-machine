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

(defn player-calls-to-end [f player-fx]
  (rest (reductions (fn [{player :db} _] (f player))
                    player-fx
                    (:player/queue (:db player-fx)))))

(defn check-current-track [expected-track fx paused?]
  (is (= expected-track (:player/playing (:db fx))))
  (is (= paused? (:player/paused (:db fx))))
  (is (= :queue (:player/selected-tab (:db fx))))
  (is (= [:new-track-playing expected-track paused?] (:dispatch fx))))

(defn check-last-fx [fxs]
  (is (= 1 (count (last fxs))) "no extra effects on last call")
  (is (= (:db (last fxs)) (:db (last (drop-last fxs)))) "data state not changed on last call"))

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
          (let [forwards (player-calls-to-end ui/player-forward playing-fx)]
            (doseq [[expected-track fx index] (map vector (rest tracks) (drop-last forwards) (range))]
              (testing (str "call number " (inc index))
                (check-current-track expected-track fx false)))
            (check-last-fx forwards)
            
            (testing "back while playing"
              (let [player-fx (last forwards)
                    backs (player-calls-to-end ui/player-back player-fx)]
                (doseq [[expected-track fx index] (map vector (rest (reverse tracks)) (drop-last backs) (range))]
                  (testing (str "call number " (inc index))
                    (check-current-track expected-track fx false)))
                (check-last-fx backs)))))
        
        (testing "forward while paused"
          (let [paused-fx (ui/player-pause (:db playing-fx))
                forwards (player-calls-to-end ui/player-forward paused-fx)]
            (doseq [[expected-track fx index] (map vector (rest tracks) (drop-last forwards) (range))]
              (testing (str "call number " (inc index))
                (check-current-track expected-track fx true)))
            (check-last-fx forwards)
            
            (testing "back while paused"
              (let [player-fx (last forwards)
                    backs (player-calls-to-end ui/player-back player-fx)]
                (doseq [[expected-track fx index] (map vector (rest (reverse tracks)) (drop-last backs) (range))]
                  (testing (str "call number " (inc index))
                    (check-current-track expected-track fx true)))
                (check-last-fx backs)))))))))

(deftest test-dequeing-tracks
  (let [tracks (take 15 @sample-tracks)
        track-ids (map :track/id tracks)
        with-enqueued (enqueue-tracks-directly @sample-data tracks)
        player (get-in with-enqueued ui/player-path)
        player-playing (:db (ui/player-play player))]
    
    (testing "While playing"
      (testing "dequeuing single track"
        (let [dequeued-fx (ui/player-dequeue-track player-playing (first track-ids)) 
              new-player (:db dequeued-fx)]
          (is (= (count (:player/queue new-player))
                (dec (count (:player/queue player-playing))))
              "Actually removes a track")
          (check-current-track (second tracks) dequeued-fx false)))
      
      (testing "dequeuing last track"
        (let [to-end-fx (player-calls-to-end ui/player-forward {:db player-playing})
              player-at-end (:db (last to-end-fx))             
              dequeued-fx (ui/player-dequeue-track player-at-end (last track-ids))
              new-player (:db dequeued-fx)]
          (is (= (count (:player/queue new-player))
                 (dec (count (:player/queue player-playing))))
              "Actually removes a track")
          (check-current-track (last (drop-last tracks))
                               dequeued-fx
                               false)))

      (testing "dequeing all tracks individually"
        (let [dequeued-fxs (reductions (fn [{p :db} id] (ui/player-dequeue-track p id))
                                       {:db player-playing}
                                       track-ids)
              dequeued-fxs (rest dequeued-fxs)]
          (testing "intermittent dequeues"
            (doseq [[expected-track fx index] (map vector (rest tracks) (drop-last dequeued-fxs) (range))]
              (testing (str "call number " (inc index))
                (check-current-track expected-track fx false))))
          (testing "properly clears at end"
            (let [cleared-fx (last dequeued-fxs)]
              (is (nil? (:player/playing (:db cleared-fx))))
              (is (:player/paused (:db cleared-fx)))
              (is (= [:all-tracks-cleared] (:dispatch cleared-fx))))))))))
