(ns western-music.handlers.map
  (:require [re-frame.core :refer [def-event]]))

(def-event
  :nation-ready
  (fn [data [_ nation]]
    (.activateNation (.-input js/map) nation)
    data))
