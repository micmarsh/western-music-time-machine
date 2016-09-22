(ns western-music.handlers
  (:require [re-frame.core :refer [reg-event-db path after debug dispatch]]
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

(reg-event-db
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

(reg-event-db
  :new-composition-data
  (path :data/raw)
  (fn [raw-data [_ composition]]
    (ui/new-composition raw-data composition)))

(defn set-value-handler [_ [_ value]] value)

(reg-event-db
  :focus-nation
  (path ui/nation-focus-path)
  set-value-handler)

(reg-event-db
  :select-nation
  (path ui/nation-selected-path)
  set-value-handler)

(reg-event-db
  :select-blank
  [ui/verify-all-data (path :data/ui)]
  (fn [ui _] (ui/reset-selection ui)))

(reg-event-db
  :select-composer
  ui/verify-all-data
  (fn [all-data [_ composer-id]]
    (let [current (ui/get-composer all-data)
          composer-id (when-not (util/string= composer-id (p/id current)) composer-id)]
      (-> all-data
          (ui/set-track-list-by-composer composer-id)
          (ui/set-composer composer-id)))))

;; Track List and Queue manipulation
(reg-event-db
  :play-track
  (path ui/player-path)
  (fn [player [_ track-id]]
    (->> track-id
         (ui/player-track-lookup player)
         (ui/player-play-track player))))

(reg-event-db
  :enqueue-track
  (path ui/player-path)
  (fn [player [_ track-id]]
    (->> track-id
         (ui/player-track-lookup player)
         (ui/player-enqueue-track player))))

(reg-event-db
  :enqueue-composer
  (fn [all-data [_ composer-id]]
    (ui/enqueue-composer all-data composer-id)))

(reg-event-db
  :play-composer
  (fn [all-data [_ composer-id]]
    (-> all-data
        (ui/enqueue-composer composer-id)
        (update-in ui/player-path ui/player-play))))

(reg-event-db
  :enqueue-nation
  (fn [all-data [_ nation-id]]
    (ui/enqueue-nation all-data nation-id)))

(reg-event-db
  :play-nation
  (fn [all-data [_ nation-id]]
    (-> all-data
        (ui/enqueue-nation nation-id)
        (update-in ui/player-path ui/player-play))))

(reg-event-db
  :dequeue-track
  [ui/verify-all-data (path ui/player-path)]
  (fn [player [_ track-id]]
    (ui/player-dequeue-track player track-id)))

(reg-event-db
  :clear-queue
  [ui/verify-all-data (path ui/player-path)]
  (fn [player _] (ui/player-clear-queue player)))

;; Player Controls
(reg-event-db
  :player-play
  [ui/verify-all-data (path ui/player-path)]
  (fn [player _] (ui/player-play player)))

(reg-event-db
  :player-pause
  (path ui/player-path)
  (fn [player _] (ui/player-pause player)))

(reg-event-db
  :player-back
  [ui/verify-all-data (path ui/player-path)]
  (fn [player _] (ui/player-back player)))

(reg-event-db
  :player-forward
  [ui/verify-all-data (path ui/player-path)]
  (fn [player _] (ui/player-forward player)))

(reg-event-db
  :current-track-ended
  (path ui/player-path)
  (fn [player _] (ui/player-track-ended player)))
