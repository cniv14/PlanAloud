FROM python:3.12-slim
WORKDIR /app
COPY server.py backup.py /app/
RUN useradd --uid 10001 --create-home planaloud && mkdir /data && chown planaloud /data
USER planaloud
ENV PLANALOUD_PORT=8765
WORKDIR /data
EXPOSE 8765
VOLUME ["/data"]
CMD ["python", "/app/server.py"]
