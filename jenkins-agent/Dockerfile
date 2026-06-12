FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        software-properties-common \
        ca-certificates \
        curl \
        git \
        openssh-client \
        openssh-server \
        openjdk-21-jdk && \
    add-apt-repository ppa:deadsnakes/ppa && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        python3.14 \
        python3.14-venv \
        python3.14-dev \
        python3-pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN ln -sf /usr/bin/python3.14 /usr/bin/python && \
    ln -sf /usr/bin/python3.14 /usr/bin/python3

WORKDIR /app

COPY requirements.txt /app/requirements.txt

RUN python -m pip install --upgrade pip && \
    python -m pip install -r /app/requirements.txt

ARG JENKINS_USER=gistest
ARG JENKINS_PASSWORD=RogueOne,

RUN useradd -m -s /bin/bash "${JENKINS_USER}" && \
    echo "${JENKINS_USER}:${JENKINS_PASSWORD}" | chpasswd && \
    mkdir -p /var/run/sshd && \
    sed -i 's/^#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config && \
    sed -i 's/^PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config && \
    sed -i 's/^#PermitRootLogin prohibit-password/PermitRootLogin no/' /etc/ssh/sshd_config && \
    sed -i 's/^#UsePAM yes/UsePAM yes/' /etc/ssh/sshd_config && \
    mkdir -p /etc/ssh/ssh_config.d && \
    printf "Host *\n    StrictHostKeyChecking no\n    UserKnownHostsFile /dev/null\n" > /etc/ssh/ssh_config.d/jenkins-insecure.conf

EXPOSE 22 8080 8081

CMD ["/usr/sbin/sshd", "-D"]
