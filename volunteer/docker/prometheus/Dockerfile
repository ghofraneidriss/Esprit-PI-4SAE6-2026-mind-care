FROM prom/prometheus:latest

# Copy custom prometheus configuration
COPY prometheus.yml /etc/prometheus/prometheus.yml

# Set default command
CMD ["--config.file=/etc/prometheus/prometheus.yml", "--storage.tsdb.path=/prometheus"]
