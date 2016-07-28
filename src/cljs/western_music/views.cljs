(ns western-music.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [western-music.protocols :as p])
  (:require-macros [western-music.util :refer [for']]))

(defn dummy-nation-list
  []
  (fn []
    (let [nations (subscribe [:all-nations])]
      [:div#fake-map
       [:div
        {:on-click #(dispatch [:select-blank])
         :key "bs1"}
        "(Blank Space)"]
       (for' [nation @nations
              :let [id (p/id nation)]]
         [:div {:on-mouse #(dispatch [:focus-nation id])
                :on-click #(dispatch [:select-nation id])
                :key id}
          (p/display nation)
          [:br]])
       [:div
        {:on-click #(dispatch [:select-blank])
         :key "bs2"}
        "(Blank Space)"]])))

(defn composition-list
  []
  (let [tracks (subscribe [:selected-tracks])
        selected-composer (subscribe [:selected-composer])]
    (fn [parent-composer]
      (when (= (p/id @selected-composer) 
               (p/id parent-composer))
        [:ul
         (for' [track @tracks
                :let [id (p/id track)]]
           [:li {:key id}
            [:div (p/display track)
             [:button
              {:on-click #(dispatch [:play-track id])}
              "PLAY"]
             [:button
              {:on-click #(dispatch [:enqueue-track id])}
              "ENQUEUE"]]])]))))

(defn composer-list
  []
  (let [composers (subscribe [:selected-composers])
        nation (subscribe [:selected-nation])]
    (fn []
      [:div
       [:ul (p/display @nation)
        (for' [composer @composers
               :let [id (p/id composer)]]
          [:li {:key id}
           [:div {:on-click #(dispatch [:select-composer id])} (p/display composer)]
           [composition-list composer]])]])))

(defn track-composer
  "SUPER BIG HACK separating notion of display value and internal ref id
   will go a long way towards fixing this"
  [track]
  (first (clojure.string/split track #" - ")))

(defn track-composition
  "SUPER BIG HACK separating notion of display value and internal ref id
   will go a long way towards fixing this"
  [track]
  (->> (clojure.string/split track #" - ")
       (rest)
       (clojure.string/join " - ")))

(defn track-queue
  []
  (let [queue (subscribe [:track-queue])
        current-track (subscribe [:current-track])]
    (fn []
      [:div
       "Play Queue"
       [:button {:on-click #(dispatch [:clear-queue])} "X"]
       [:ul
        (for' [track @queue
               :let [id (p/id track)]]
          [:li {:key id}
           [:div (p/display track)
            (when-not (= id (p/id @current-track))
              [:button {:on-click #(dispatch [:play-track id])}
               "PLAY"])
            [:button
             {:on-click #(dispatch [:dequeue-track id])}
             "X"]]])]])))

(defn player-controls
  []
  (let [paused? (subscribe [:paused?])]
    (fn []
      [:div
       [:button {:on-click #(dispatch [:player-back])} "Back"]
       (if @paused?
         [:button {:on-click #(dispatch [:player-play])} "Play"]
         [:button {:on-click #(dispatch [:player-pause])} "Pause"])
       [:button {:on-click #(dispatch [:player-forward])} "Forward"]])))

(defn wmtm-app []
  [:div
   [dummy-nation-list]
   [composer-list]
   [player-controls]
   [track-queue]])
