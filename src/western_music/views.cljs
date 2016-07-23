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
            composition])]))))

(defn composer-list
  []
  (let [composers (subscribe [:selected-composers])]
    (fn []
      [:div#fake-track-list
       [:ul
        (for' [composer @composers]
          [:li {:key composer}
           [:div {:on-click #(dispatch [:select-composer composer])} composer]
           [composition-list composer]])]])))

(defn wmtm-app []
  [:div
   [dummy-nation-list]
   [composer-list]])
