(ns western-music.core
  (:require [western-music.views :refer [wmtm-app]]
            [western-music.handlers]
            [western-music.subscriptions]
            [re-frame.core :refer [dispatch-sync]]
            [reagent.core :as reagent]))

(def player)

(defn player-ready []
  (println "player ready"))

(defn player-state-change []
  (println "player state change"))

(def player-options
  #js {:height "240"
       :width "300"
       :events {:onReady player-ready
                :onStateChange player-state-change}})

(defn init-player []
  (let [Player (.-Player js/YT)]
    (set! player (Player. "youtube-player" player-options))))

(defn ^:export main
  []
  (dispatch-sync [:initialize-data])
  (reagent/render [wmtm-app] (.getElementById js/document "app")
                  (fn try-init []
                    (if (boolean js/YouTubeReady)
                      (init-player)
                      (js/setTimeout try-init 1000)))))
