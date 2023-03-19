(ns daveduthie.load-shedding-calendar
  (:require [daveduthie.load-shedding-calendar.ical :as ical]
            [daveduthie.load-shedding-calendar.schedule :as schedule]
            [daveduthie.load-shedding-calendar.scrape :as scrape]
            [integrant.core :as ig]
            [ring.adapter.jetty :as jetty])
  (:gen-class))

(defn valid-interval? [{:keys [start end]}] (.isBefore start end))

(defn intersection
  [{s1 :start, e1 :end} {s2 :start, e2 :end}]
  (let [later-start (if (.isAfter s1 s2) s1 s2)
        earlier-end (if (.isBefore e1 e2) e1 e2)
        intvl {:start later-start, :end earlier-end}]
    (when (valid-interval? intvl) intvl)))

(defn load-shedding
  [zone]
  (let [schedule (scrape/ct-schedule)]
    (mapcat (fn [{:keys [stage start guess], :as intvl}]
              (let [schedule-for-date (schedule/load-shedding-for-zone
                                        stage
                                        (.toLocalDate start)
                                        zone)]
                (into []
                      (comp (keep (partial intersection intvl))
                            (map #(assoc %
                                    :stage stage
                                    :guess guess)))
                      schedule-for-date)))
      schedule)))

(defn- event-title
  [guess stage]
  (str (format "%sLoad Shedding (Stage %s)" (if guess "[?] " "") stage)))

(defn app
  ;; TODO: get zone as query param
  [_req]
  {:status 200,
   :headers {"Content-Type" "text/calendar"},
   :body (ical/ical (map (fn [{:keys [start end stage guess]}]
                           (ical/event start end (event-title guess stage)))
                      (load-shedding 2)))})

(defmethod ig/init-key ::http-server
  [_ {:keys [port]}]
  (jetty/run-jetty #(app %) {:port port, :join? false}))

(defmethod ig/halt-key! ::http-server [_ srv] (.stop srv))

(comment
  (user/system)
  (spit "x.ics"
        (ical/ical (map (fn [{:keys [start end stage]}]
                          (ical/event start
                                      end
                                      (str (format "Load Shedding (Stage %s)"
                                                   stage))))
                     (load-shedding 2)))))

