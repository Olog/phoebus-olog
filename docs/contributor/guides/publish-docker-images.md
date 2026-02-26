# Publishing Docker Images to the Docker registry

Docker images are published to the GitHub Container Registry via GitHub actions. This is triggered by:
  - push to master

Images are published to the `ghcr.io/<org name>/<group id>-<artifact id>` registry under the following tags:
  - `latest`
  - `<version>`
  - `<version>-<timestamp>`

This tag information is extracted from the Maven POM (via the `help` plugin). In order to avoid defining all of this 
in the GitHub actions themselves, the generation of these tag names is delegated to shell scripts in the `scripts` folder. 

If you need to debug issues related to registry/tag names outside of GitHub's CI/CD environment, 
you can run these scripts locally:

```bash
# setup your environment with GitHub's enviroment vars and files
source ./scripts/setup-locally.sh

# Build the tag names from Maven etc and write them to PROJECT_INFO.txt
./scripts/write-project-info.sh

# Append the vars in PROJECT_INFO.txt to the GitHub environment file 
# (setup-locally.sh uses THE_GITHUB_ENV_FILE.txt, but in GH CI/CD 
# it is unique per run)
# This file should match PROJECT_INFO.txt
./scripts/set-github-env.sh
```

What you should see is:
  - registry and tag names are valid; e.g. are all lowercase and use dashes instead of spaces
  - registry and tag names should not be null/empty/unrelated to this project

If you would like to read up more on how passing environment variables between steps in a GitHub action works, 
please see their docs on [Job Outputs](https://docs.github.com/en/actions/using-jobs/defining-outputs-for-jobs). 
