(ns western-music.views
  (:require [re-frame.core :refer [subscribe dispatch]])
  (:require-macros [western-music.util :refer [for']]))

(defn dummy-nation-list
  []
  (fn []
    (let [nations (subscribe [:all-nations])]
      [:div#fake-map
       [:div
        {:on-click #(dispatch [:select-blank])}
        "(Blank Space)"]
       (for' [nation @nations]
         [:div {:on-mouse #(dispatch [:focus-nation nation])
                :on-click #(dispatch [:select-nation nation])
                :key nation}
          nation
          [:br]])
       [:div
        {:on-click #(dispatch [:select-blank])}
        "(Blank Space)"]])))

(defn composition-list
  []
  (let [compositions (subscribe [:selected-compositions])
        selected-composer (subscribe [:selected-composer])]
    (fn [parent-composer]
      (when (= @selected-composer parent-composer)
        [:ul
         (for' [composition @compositions]
           [:li {:key composition}
            [:div composition
             [:button
              {:on-click #(dispatch [:play-composition
                                     parent-composer
                                     composition])}
              "PLAY"]
             [:button
              {:on-click #(dispatch [:enqueue-composition
                                     parent-composer
                                     composition])}
              "ENQUEUE"]]])]))))

(defn composer-list
  []
  (let [composers (subscribe [:selected-composers])
        nation (subscribe [:selected-nation])]
    (fn []
      [:div
       [:ul @nation
        (for' [composer @composers]
          [:li {:key composer}
           [:div {:on-click #(dispatch [:select-composer composer])} composer]
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
  (apply str
         (clojure.string/join " - "
                              (rest (clojure.string/split track #" - ")))))

(defn track-queue
  []
  (let [queue (subscribe [:track-queue])
        current-track (subscribe [:current-track])]
    (fn []
      [:div
       "Play Queue"
       [:ul
        (for' [track @queue]
          [:li {:key track}
           [:div track
            (when-not (= track @current-track)
              [:button {:on-click #(dispatch [:play-composition
                                              (track-composer track)
                                              (track-composition track)])}
               "PLAY"])
            [:button
             {:on-click #(dispatch [:dequeue-track track])}
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
