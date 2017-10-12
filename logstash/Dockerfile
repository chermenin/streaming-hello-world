FROM docker.elastic.co/logstash/logstash:5.4.0
COPY logstash-filter-elapsed.zip /usr/share/logstash/logstash-filter-elapsed.zip
RUN logstash-plugin install file:///usr/share/logstash/logstash-filter-elapsed.zip
USER root
RUN mkdir -p /var/log/logstash
RUN chmod 777 /var/log/logstash
