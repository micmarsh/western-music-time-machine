(ns western-music.handlers.youtube
  (:require [re-frame.core :refer [reg-event-fx]]
            [western-music.lib.ui :as ui]
            [youtube-fx.core]))

(def ^:const +video-ended+ 0)
(def ^:const +video-playing+ 1)
(def ^:const +video-paused+ 2)

(defmulti player-state-change (fn [e] (.-data e)))

(defmethod player-state-change +video-ended+
  [_]
  {:dispatch [:current-track-ended]})

(defmethod player-state-change +video-playing+
  [_]
  {:dispatch [:player-play]})

(defmethod player-state-change +video-paused+
  [_]
  {:dispatch [:player-pause]})

(defmethod player-state-change :default
  [e]
  (println "player state change" (.-data e))
  {})

(reg-event-fx
 :player-state-change
 (fn [_ [_ e]]
   (player-state-change e)))

(def ^:const player-options
  {:height "350"
   :width "480"
   :events {:on-state-change [:player-state-change]}})

(reg-event-fx
 :initialize-player
 (fn [_ _]
   (if (boolean js/YouTubeReady)
     {:youtube/initialize-player [:youtube-player player-options]}
     {:dispatch-later [{:ms 1000 :dispatch [:initialize-player]}]})))

(reg-event-fx
 :new-track-playing
 (fn [_ [_ {id :track/youtube-id} paused?]]
   (if paused?
     {:youtube/cue-video-by-id [:youtube-player id]}
     {:youtube/load-video-by-id [:youtube-player id]})))

(reg-event-fx
 :current-track-playing
 (constantly {:youtube/play-video :youtube-player}))

(reg-event-fx
 :current-track-paused
 (constantly {:youtube/pause-video :youtube-player}))

(reg-event-fx
 :all-tracks-cleared
 (constantly {:youtube/cue-video-by-id [:youtube-player ""]}))
