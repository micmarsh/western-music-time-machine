(ns western-music.handlers
  (:require [re-frame.core :refer [def-event path after debug dispatch]]
            [western-music.lib.composition :as composition]
            [western-music.protocols :as p]
            [western-music.spec :as spec]
            [western-music.lib.ui :as ui]
            [western-music.util :as util]
            [clojure.spec :as s]
            [ajax.core :refer [GET]]
            [ajax.edn :as edn]
            ;; implemenation-specific handlers
            [western-music.handlers.youtube]
            [western-music.handlers.map]))

(def current-seconds #(.getTime (js/Date.)))

(defn cache-bust [url]
  (->> (current-seconds)
       (str "?&_=")
       (str url)))

(def-event
  :initialize-data
  ui/verify-all-data
  (fn [data _]
    (GET (cache-bust "edn/compositions.edn")
         {:handler (fn [initial-data]
                     (doseq [composition initial-data]
                       (dispatch [:new-composition-data composition])
                       (dispatch [:nation-ready (composition/nation-id composition)])))
          :response-format (edn/edn-response-format)})
    ui/initial-data))

(def-event
  :new-composition-data
  (path :data/raw)
  (fn [raw-data [_ composition]]
    (ui/new-composition raw-data composition)))

(defn set-value-handler [_ [_ value]] value)

(def-event
  :focus-nation
  (path ui/nation-focus-path)
  set-value-handler)

(def-event
  :select-nation
  (path ui/nation-selected-path)
  set-value-handler)

(def-event
  :select-blank
  [ui/verify-all-data (path :data/ui)]
  (fn [ui _] (ui/reset-selection ui)))

(def-event
  :select-composer
  ui/verify-all-data
  (fn [all-data [_ composer-id]]
    (let [current (ui/get-composer all-data)
          composer-id (when-not (util/string= composer-id (p/id current)) composer-id)]
      (-> all-data
          (ui/set-track-list-by-composer composer-id)
          (ui/set-composer composer-id)))))

;; Track List and Queue manipulation
(def-event
  :play-track
  (path ui/player-path)
  (fn [player [_ track-id]]
    (->> track-id
         (ui/player-track-lookup player)
         (ui/player-play-track player))))

(def-event
  :enqueue-track
  (path ui/player-path)
  (fn [player [_ track-id]]
    (->> track-id
         (ui/player-track-lookup player)
         (ui/player-enqueue-track player))))

(def-event
  :enqueue-composer
  (fn [all-data [_ composer-id]]
    (ui/enqueue-composer all-data composer-id)))

(def-event
  :play-composer
  (fn [all-data [_ composer-id]]
    (-> all-data
        (ui/enqueue-composer composer-id)
        (update-in ui/player-path ui/player-play))))

(def-event
  :enqueue-nation
  (fn [all-data [_ nation-id]]
    (ui/enqueue-nation all-data nation-id)))

(def-event
  :play-nation
  (fn [all-data [_ nation-id]]
    (-> all-data
        (ui/enqueue-nation nation-id)
        (update-in ui/player-path ui/player-play))))

(def-event
  :dequeue-track
  [ui/verify-all-data (path ui/player-path)]
  (fn [player [_ track-id]]
    (ui/player-dequeue-track player track-id)))

(def-event
  :clear-queue
  [ui/verify-all-data (path ui/player-path)]
  (fn [player _] (ui/player-clear-queue player)))

;; Player Controls
(def-event
  :player-play
  [ui/verify-all-data (path ui/player-path)]
  (fn [player _] (ui/player-play player)))

(def-event
  :player-pause
  (path ui/player-path)
  (fn [player _] (ui/player-pause player)))

(def-event
  :player-back
  [ui/verify-all-data (path ui/player-path)]
  (fn [player _] (ui/player-back player)))

(def-event
  :player-forward
  [ui/verify-all-data (path ui/player-path)]
  (fn [player _] (ui/player-forward player)))

(def-event
  :current-track-ended
  (path ui/player-path)
  (fn [player _] (ui/player-track-ended player)))
