[![CircleCI](https://circleci.com/gh/atlassian/virtual-users.svg?style=svg)](https://circleci.com/gh/atlassian/virtual-users)

# Virtual Users
Virtual users simulate end users using browsers to communicate with a web application.
They aim to be realistic, low-config and robust.

## Features

- Downloads and configures ChromeDriver
- Uses multiple copies of ChromeDriver in parallel
- Configures Jira
- Runs scenario consisting of WebDriver actions
- Provides a predictable source of randomness for each virtual user
- Diagnoses action failures with screenshots, HTML dumps and Java stack traces
- Streams action metrics to the disk in real time
- Observes the fairness of load balancing in Jira Data Center by counting visits to specific nodes

## Requirements

- network access to the target Jira instance
- [Google Chrome](https://www.google.com/chrome/) 69-80
- [JDK](http://openjdk.java.net/) 8 - 11

## Reporting issues

We track all the changes in a [public issue tracker](https://ecosystem.atlassian.net/secure/RapidBoard.jspa?rapidView=457&projectKey=JPERF).
All the suggestions and bug reports are welcome.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License
Copyright (c) 2018 Atlassian and others.
Apache 2.0 licensed, see [LICENSE.txt](LICENSE.txt) file.
