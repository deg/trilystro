(ns vuagain.chromex.popup.views
  (:require
   [chromex.ext.runtime :as runtime :refer-macros [connect]]
   [chromex.protocols :refer [post-message!]]
   [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [reagent.core :as reagent]
   [soda-ash.core :as sa]
   [sodium.core :as na]
   [sodium.extensions :as nax]
   [trilib.firebase :as fb]
   [trilib.fsm :as fsm]))


(defn bg-msg [message]
  (post-message! (<sub [:background-port])
                 (clj->js (assoc message :app "VuAgain"))))


(defn header-bar []
  [nax/app-header "VuAgain"])

(defn TOS-page []
  [na/form {:class-name "spaPage", :id "TOSForm"}
   [:h4 "VuAgain Terms of Service"]
   [:p "VuAgain is in alpha pre-release, and may not yet be fully reliable."]
   [:p "Do not yet use VuAgain to store any information that you cannot afford to\nlose, or that you must keep confidential."]
   [:p "We plan to upgrade rapidly. Please contact our"
    [:a {:href "mailto:info@vuagain.com"}
     "support desk"]" with your comments\nand suggestions."]
   [:h5 "How to Use"]
   [:p "VuAgain helps you save notes about web pages you want to remember.\nUnlike bookmarking services, VuAgain does not force\nyou to look for your saved notes. Instead, they appear in your regular\nGoogle search results."]
   [:p "You can save two kinds of notes:"]
   [:ul
    [:li "Private notes are seen only by you"]
    [:li "Public comments can be seen by everyone"]]])

(defn logged-out-page []
  [:div
   [na/form {:class-name "spaPage", :id "anonymousForm"}
    [:p "You do not seem to be logged in with a chrome identity."]
    [:p "VuAgain depends on features supplied by Chrome and cannot start until you are\nlogged in."]
    [:p "VuAgain will not work if you are using a non-Chrome browser or are logged in\nanonymously."]
    [:p "If you are seeing this message in other circumstances, please\ncontact our "
     [:a {:href "mailto:info@vuagain.com"} "support desk"]"."]]
   [TOS-page true]])



(defn lystro-editor [url lystro-atom original-lystro]
  (let [partial-tag-text (reagent/atom "")]
    (fn [url lystro-atom original-lystro]
      (when-not (nil? @lystro-atom)
        (let [default-public? (<sub [::fb/user-settings [:default-public?] false])
              original-public? (if (nil? (:public? original-lystro))
                                 default-public?
                                 (:public? original-lystro))
              public? (if (nil? (:public? @lystro-atom))
                        default-public?
                        (:public? @lystro-atom))
              action (if (empty? original-lystro) "Add" "Edit")]
          [na/form {:class-name "spaPage", :id "vaForm"}
           [:div {:class "form-group"}
            [:div (str action " Lystro for ") [:b (<sub [:title])] ", from " [:em url]]
            [nax/labelled-field
             :label "Tags:"
             :inline? true
             :content [nax/tag-adder {:partial-tag-text        partial-tag-text
                                      :all-tags-sub            [::fb/all-tags]
                                      :selected-tags-sub       #(:tags @lystro-atom)
                                      :set-selected-tags-event #(swap! lystro-atom assoc :tags %)}]]
            [nax/labelled-field
             :label "Text:"
             :content [nax/native-text-area
                       {:rows 3
                        :placeholder "Description..."
                        :value (or (:text @lystro-atom) "")
                        :on-change #(swap! lystro-atom assoc :text (-> % .-target .-value))}]]
            [nax/labelled-field
             :label "Visibility:"
             :content [sa/Checkbox {:label "Public"
                                    :checked public?
                                    :on-change (na/value->atom-fn lystro-atom {:default false :assoc-path [:public?]})}]]
            (let [connected? (:firebase/connected? (<sub [:firebase/connection-state]))]
              [:span
               [na/form-button {:on-click #(js/window.close) :content "Cancel"}]
               [na/form-button {:disabled? (or (not (empty? @partial-tag-text))
                                               (empty? (:text @lystro-atom))
                                               (not connected?))
                                :on-click (fn []
                                            (>evt [::fb/commit-lystro (assoc @lystro-atom
                                                                             :url (<sub [:url])
                                                                             :owner (<sub [::fb/uid])
                                                                             :original-public? original-public?
                                                                             :public? public?)
                                                   :on-success #(js/window.close)]))
                                :icon (if connected? "add" "wait")
                                :content (if connected?
                                           (str "Save " (if public? "public" "private"))
                                           "(offline)")
                                :positive? true}]])]])))))


(defn logged-in-page [url relevant-lystros]
  (let [lystro-atom (reagent/atom nil)]
    (fn [url relevant-lystros]
      (let [original-lystro
            ;; [TODO] Need to check for multiple existing Lystros on this URL. But, I
            ;;        don't know yet what the correct behavior should be.
            (or (first relevant-lystros) {})]
        (reset! lystro-atom original-lystro)
        [lystro-editor url lystro-atom original-lystro]))))


(defn footer-bar []
  (let [user (<sub [::fb/user])]
    [:div {:class "panel-footer", :style {:margin-top "15px"}}
     [na/button {:class-name "btn btn-social",
                 :id (if user "logout" "login")
                 :content (if user (str "logout: " (<sub [::fb/user-name])) "login")
                 :on-click #(bg-msg {:command (if user "sign-out" "sign-in")})}]
     [na/button {:class-name "btn",
                 :id "open-VuAgain"
                 :content "full VuAgain"
                 :on-click #(js/window.open "http://trilystro.vuagain.com")}]
     [:small
      [:p {:class "alignRight", :id "versionString"}
       "VuAgain v0.3.0"]]]))


;;; We want to keep the Firebase ":on" subscriptions active, so need to mount them in the
;;; main panel. But, we don't want anything to show. We could use a display:none div, but
;;; this head-fake is more elegant, and seems to work works.
;;; [TODO] ^:export is probably not needed, but I've not tested removing it. See
;;;        discussion in Slack #clojurescript channel Sept 6-7 2017.
(defn ^:export null-op [x] "")

(defn popup []
  [:div
   [header-bar]
   [:div {:class "container"}
    (if (<sub [::fb/user])
      (let [open-state ;; Subs that should be held open for efficiency
            [(<sub [:firebase/on-value {:path (fb/private-fb-path [:lystros])}])
             (<sub [:firebase/on-value {:path (fb/private-fb-path [:user-settings])}])
             (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:lystros])}])
             (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:user-details])}]) ;; [TODO][ch94] rename
             ]

            url (<sub [:url])
            relevant-lystros (<sub [::fb/lystros-of-url url])]
        (null-op open-state)
        [logged-in-page url relevant-lystros])
      [logged-out-page])]
   [footer-bar]])
