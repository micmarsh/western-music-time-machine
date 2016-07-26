(ns western-music.core
  (:require [western-music.views :refer [wmtm-app]]
            [western-music.handlers]
            [western-music.subscriptions]
            [re-frame.core :refer [dispatch-sync]]
            [reagent.core :as reagent]))

(defn ^:export main
  []
  (dispatch-sync [:initialize-data])
  (reagent/render [wmtm-app] (.getElementById js/document "app")))
