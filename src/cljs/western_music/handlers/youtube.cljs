(ns western-music.handlers.youtube
  (:require [re-frame.core :refer [def-event dispatch]]
            [western-music.lib.ui :as ui]))

(def player)

(def println' #(.log js/console %))

(defn player-ready []
  (println' "player ready"))

(def ^:const +video-ended+ 0)
(def ^:const +video-playing+ 1)
(def ^:const +video-paused+ 2)

(defmulti player-state-change (fn [e] (.-data e)))

(defmethod player-state-change +video-ended+
  [_]
  (dispatch [:current-track-ended]))

(defmethod player-state-change +video-playing+
  [_]
  (dispatch [:player-play]))

(defmethod player-state-change +video-paused+
  [_]
  (dispatch [:player-pause]))

(defmethod player-state-change :default
  [e]
  (println' (.-data e))
  (println' "player state change"))

(def player-options
  (clj->js
   {:height "240"
    :width "300"
    :events {:onReady player-ready
             :onStateChange player-state-change}
    :playerVars {:controls 0}}))

(defn initialize [& _]
  (if (boolean js/YouTubeReady)
    (let [Player (.-Player js/YT)]
      (set! player (Player. "youtube-player" player-options)))
    (js/setTimeout initialize 1000)))

(def-event
  :initialize-player
  (fn [data _]
    (initialize)
    data))

(def-event
  :new-track-playing
  (fn [data [_ {id :track/youtube-id} paused?]]
    (when (nil? (.-loadVideoById player))) ;; TODO something elegant
    ;; to handle figwheel reloading action
    (if paused?
      (.cueVideoById player id)
      (.loadVideoById player id))
    data))

(def-event
  :current-track-playing
  (fn [data _]
    (.playVideo player)
    data))

(def-event
  :current-track-paused
  (fn [data _]
    (.pauseVideo player)
    data))
