
Config from Vincent

{
      "type": "siri-sx-updater",
      "frequency": "60s",
      "url": "https://api.entur.io/realtime/v1/services-cache",
      "timeout": "20s",
      "feedId": "RB",
      "blockReadinessUntilInitialized": true,
      "headers": {
        "ET-Client-Name": "local-SX"
      }
    },
    {
      "type": "siri-et-google-pubsub-updater",
      "feedId": "RB",
      "reconnectPeriod": "5s",
      "initialGetDataTimeout": "1m20s",
      "topicProjectName": "ent-anshar-tst",
      "topicName": "protobuf.estimated_timetables",
      "subscriptionProjectName": "ent-otp2-tst",
      "dataInitializationUrl": "http://localhost:8091/et",
      "fuzzyTripMatching": true
    }

KUBERNETES DOC

https://kubernetes.io/docs/reference/kubectl/quick-reference/


** List configured clusters **
> kubectx
jp
main

** Select active cluster **
> kubectx main
or
> kubectx gke_ent-kub-tst_europe-west1_kub-ent-tst-001


**List clusters**
> kubectl config get-clusters
NAME
gke_ent-kub-tst_europe-west1_kub-ent-tst-001
gke_ent-kub-tst_europe-west4_kub-ent-jp-tst-001

** List RT cache pods **

> kubectl get pods -n=realtime-cache                                                                                                                     1:19:49
NAME                              READY   STATUS    RESTARTS   AGE
realtime-cache-564468d4d7-d7jxb   1/1     Running   0          9d

** Set up port forwarding **

> kubectl port-forward realtime-cache-564468d4d7-d7jxb 8091:8080 -n=realtime-cache --cluster gke_ent-kub-tst_europe-west1_kub-ent-tst-001                                                                       1:23:45
Forwarding from 127.0.0.1:8091 -> 8080
Forwarding from [::1]:8091 -> 8080
