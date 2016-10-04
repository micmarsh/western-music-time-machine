(ns western-music.handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx path after dispatch]]
            [western-music.lib.composition :as composition]
            [western-music.protocols :as p]
            [western-music.spec :as spec]
            [western-music.lib.ui :as ui]
            [western-music.util :as util]
            [clojure.spec :as s]
            [day8.re-frame.http-fx]
            [ajax.edn :as edn]
            ;; implemenation-specific handlers
            [western-music.handlers.youtube]
            [western-music.handlers.map]))

(def current-seconds #(.getTime (js/Date.)))

(defn cache-bust [url]
  (->> (current-seconds)
       (str "?&_=")
       (str url)))

(reg-event-fx
  :initialize-data
  ui/verify-all-data
  (fn [_ _]
    {:db ui/initial-data
     :http-xhrio {:method :get
                  :uri (cache-bust "edn/compositions.edn")
                  :body {}
                  :response-format (edn/edn-response-format)
                  :on-success [:compositions-from-server]}}))

(reg-event-fx
 :compositions-from-server
 (fn [_ [_ data]]
   {:dispatch-n
    (mapcat (juxt (comp (partial vector :nation-ready) composition/nation-id)
                  (partial vector :new-composition-data))
            data)}))

(reg-event-db
  :new-composition-data
  (path :data/raw)
  (fn [raw-data [_ composition]]
    (ui/new-composition raw-data composition)))

(reg-event-db
  :focus-nation
  (path :data/ui)
  (fn [ui [_ nation]]
    (ui/focus-nation ui nation)))

(reg-event-db
  :select-nation
  (path :data/ui)
  (fn [ui [_ nation]]
    (-> ui
        (ui/select-nation nation)
        (ui/set-tab :selection))))

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
(reg-event-fx
  :play-track
  (path ui/player-path)
  (fn [{player :db} [_ track-id]]
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

(reg-event-fx
  :play-composer
  (fn [{db :db} [_ composer-id]]
    {:db (ui/enqueue-composer db composer-id)
     :dispatch [:player-play]}))

(reg-event-db
  :enqueue-nation
  (fn [all-data [_ nation-id]]
    (ui/enqueue-nation all-data nation-id)))

(reg-event-fx
  :play-nation
  (fn [{db :db} [_ nation-id]]
    {:db (ui/enqueue-nation db nation-id)
     :dispatch [:player-play]}))

(reg-event-fx
  :dequeue-track
  [ui/verify-all-data (path ui/player-path)]
  (fn [{player :db} [_ track-id]]
    (ui/player-dequeue-track player track-id)))

(reg-event-fx
  :clear-queue
  [ui/verify-all-data (path ui/player-path)]
  (fn [{player :db} _] (ui/player-clear-queue player)))

;; Player Controls
(reg-event-fx
  :player-play
  [ui/verify-all-data (path ui/player-path)]
  (fn [{player :db} _] (ui/player-play player)))

(reg-event-fx
  :player-pause
  (path ui/player-path)
  (fn [{player :db} _] (ui/player-pause player)))

(reg-event-fx
  :player-back
  [ui/verify-all-data (path ui/player-path)]
  (fn [{player :db} _] (ui/player-back player)))

(reg-event-fx
  :player-forward
  [ui/verify-all-data (path ui/player-path)]
  (fn [{player :db} _] (ui/player-forward player)))

(reg-event-fx
  :current-track-ended
  (path ui/player-path)
  (fn [{player :db} _] (ui/player-track-ended player)))


(reg-event-db
 :select-tab
 (path :data/ui)
 (fn [db [_ tab]]
   (ui/set-tab db tab)))
