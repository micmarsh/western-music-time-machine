(ns western-music.handlers.map
  (:require [re-frame.core :refer [def-event]]))

(def-event
  :nation-ready
  (fn [data [_ nation]]
    (let [map (.-input js/map)]
      (when-not (.isActivated map nation)
        (.activateNation map nation)))
    data))
