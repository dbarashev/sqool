FROM gradle:5.0
USER root
RUN mkdir /workspace
ADD build.gradle /workspace
ADD src /workspace
RUN cd /workspace && gradle test