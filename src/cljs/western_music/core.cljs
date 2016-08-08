(ns western-music.core
  (:require [western-music.views :refer [wmtm-app]]
            [western-music.handlers]
            [western-music.subscriptions]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [reagent.core :as reagent]))

(defn ^:export main
  []
  (reagent/render [wmtm-app] (.getElementById js/document "app")
                  (fn []
                    (.initialize js/map)
                    (dispatch [:initialize-data])
                    (dispatch [:initialize-player]))))
