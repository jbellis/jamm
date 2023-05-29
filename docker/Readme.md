<!--
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
-->

# Docker CI Testing

Docker file in this directory is used to build image used by CircleCI.

## Building and Publishing Images

Build image from the parent directory using the following command (currently building only amd and Ekaterina
publish it as it is in her dockerhub repo for initial test purposes):

`docker buildx build --platform linux/amd64 -t edimitrova86/jamm-testing-ubuntu2204:$(date +"%Y%m%d") -t edimitrova86/jamm-testing-ubuntu2204:latest -f ubuntu2204.docker --push .`

Please make sure to always tag also by date, so we can go back to that version in case anything breaks after the next update!

We are using Docker Hub for storing published images. 
