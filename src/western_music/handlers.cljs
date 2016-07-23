(ns western-music.handlers
  (:require [western-music.data :refer [initial-data]]
            [re-frame.core :refer [def-event path debug]]))

(def-event
  :initialize-data
  (constantly {:raw initial-data
               :ui {}}))

(defn set-value-handler [_ [_ value]] value)

(def-event
  :focus-nation
  [(path :ui :nation :mouse-on)]
  set-value-handler)

(def-event
  :select-nation
  [(path :ui :nation :selected)]
  set-value-handler)

(def-event
  :select-blank
  (path :ui)
  (fn [ui _]
   (dissoc ui :composer :nation)))

(def-event
  :select-composer
  (path :ui :composer)
  set-value-handler)

;; TODO Time selection is the next UI element to incorporate
