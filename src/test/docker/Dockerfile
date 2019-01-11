FROM openjdk:8u181-jdk

RUN apt-get update
RUN apt-get install -y unzip libx11-xcb1 libxcomposite1 libxdamage1 libxi6 libxtst6 libnss3 libcups2 libxss1 libxrandr2
RUN apt-get install -y libasound2 libpango1.0 libatk1.0-0 libatk-bridge2.0 libgtk-3-0
RUN rm -rf /var/lib/apt/lists/* /var/cache/apt/*
RUN wget https://www.googleapis.com/download/storage/v1/b/chromium-browser-snapshots/o/Linux_x64%2F576753%2Fchrome-linux.zip?alt=media -O chromium.zip
RUN unzip chromium.zip
RUN rm chromium.zip
RUN ln -s `pwd`/chrome-linux/chrome /usr/bin/chrome

LABEL version=0.0.2