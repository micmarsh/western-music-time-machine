(ns western-music.core
  (:require [western-music.views :refer [wmtm-app]]
            [western-music.handlers]
            [western-music.subscriptions]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [reagent.core :as reagent]))

(defn set-map-listeners! []
  (set! (.-onNationFocus (.-listeners js/map))
        #(dispatch [:focus-nation %]))
  (set! (.-onNationClick (.-listeners js/map))
        #(dispatch [:select-nation %])))

(defn ^:export main
  []
  (reagent/render [wmtm-app] (.getElementById js/document "app")
                  (fn []
                    (set-map-listeners!)
                    (.click (js/$ "#selection-tab"))
                    (.initialize js/map)
                    (dispatch [:initialize-data])
                    (dispatch [:initialize-player]))))
