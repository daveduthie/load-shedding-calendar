(ns daveduthie.load-shedding-calendar.scrape
  (:require [clojure.core.memoize :as memo]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
  (:import (java.time LocalTime MonthDay Year ZoneId)
           (java.time.format DateTimeFormatter)))

(def ^:private ct-load-shedding-url
  "https://www.capetown.gov.za/Family%20and%20home/Residential-utility-services/Residential-electricity-services/Load-shedding-and-outages")

(def ^:private ct-load-shedding-html
  (memo/ttl (fn []
              (tap> ::fetching-html)
              (html/html-resource (java.net.URL. ct-load-shedding-url)))
            :ttl/threshold
            60000))

(def ^:private load-shed-line-re
  #"Stage (\d)(?: \(no load-shedding\))?(: (underway until|\d{2}:\d{2}) (?:- )?(\d{2}:\d{2}))?")

(def ^:private date-pattern (DateTimeFormatter/ofPattern "dd MMMM"))

(defn- parse-date
  [date-str]
  (-> (MonthDay/parse date-str date-pattern)
      (.atYear (.getValue (Year/now)))))

(def ^:private jhb-zone (ZoneId/of "Africa/Johannesburg"))

;; TODO: find another "underway until" example and adapt
(defn- parse-schedule-text
  [line]
  ;; TODO: improve regex to drop _start-end group
  (when-let [[_ stage _start-end start end] (re-matches load-shed-line-re line)]
    {:stage stage, :start start, :end end, :raw/line line}))

(defn parse-times
  [date {:as interval, :keys [start end]}]
  (let [start-time (LocalTime/parse (or start "00:00"))
        end-time (LocalTime/parse (or end "23:59"))]
    (-> interval
        (assoc :date (parse-date date))
        (update :stage #(Integer/parseInt %))
        (assoc :start (-> (parse-date date)
                          (.atTime start-time)
                          (.atZone jhb-zone))
               :end (-> (parse-date date)
                        (cond-> (.isBefore end-time start-time) (.plusDays 1))
                        (.atTime end-time)
                        (.atZone jhb-zone))))))

(defn- parse-schedule-for-date
  [date text]
  (eduction (comp (keep parse-schedule-text) (map (partial parse-times date)))
            (str/split-lines text)))

(defn- parse-ct-schedule
  "Give up and just parse times manually"
  []
  (loop [acc []
         [x y & more] (-> (ct-load-shedding-html)
                          (html/select [:div.section-pull])
                          first
                          :content)]
    (cond (not more) acc
          (= (:tag x) :strong)
            (recur (into acc (parse-schedule-for-date (html/text x) y)) more)
          :else (recur acc more))))

(defn ct-schedule
  []
  (let [schedule (parse-ct-schedule)
        extend-to-end-of-day (fn [intvl]
                               (update intvl
                                       :end
                                       (fn [zdt]
                                         (-> zdt
                                             (.withHour 23)
                                             (.withMinute 59)))))
        whole-day (fn [stage date]
                    {:stage stage,
                     :date date,
                     :start (-> date
                                (.atTime (LocalTime/of 0 0))
                                (.atZone jhb-zone)),
                     :end (-> date
                              (.atTime (LocalTime/of 23 59))
                              (.atZone jhb-zone)),
                     :guess true,
                     :raw/line "Synthetic! Extended last stage to more days"})
        {:keys [stage date]} (last schedule)]
    (-> schedule
        ;; TODO: maybe twitter is a more up-to-date source of info?
        (update (dec (count schedule)) extend-to-end-of-day)
        (into [(whole-day stage (.plusDays date 1))
               (whole-day stage (.plusDays date 2))
               (whole-day stage (.plusDays date 3))]))))

(comment
  (do (portal.api/clear) (tap> (parse-ct-schedule))))

(comment
  (import '(java.time ZonedDateTime))
  (java.util.Date/from (.toInstant (ZonedDateTime/now)))
  (tap> (parse-times "21 March" (parse-schedule-text "Stage 1: 16:00 - 22:00")))
  :.)
