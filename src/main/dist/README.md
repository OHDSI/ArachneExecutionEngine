# Using

sudo run_build.sh 

- -a distribution architecture **i386** or **amd64**, default is amd64
- -d Ubuntu release name, default is **trusty**, for complete list look at Ubuntu Wiki (https://wiki.ubuntu.com/Releases).
- -b Directory where distribution build would be running
- -f Output archive filename
- -h prints usage

## Adding GitHub Personal Access Token

GitHub limits a number of requests sending from unauthorized host and you may meet
this issue during building process.
Fortunately GitHub allows to use personal access token and then limits would be not so strict.

With R you could use `usethis` to get and store PAT.
1. Run `usethis::browse_github_token()`, this command opens your browser 
and generates PAT. Just copy it.
1. And then run `usethis::edit_r_environ()`
1. This opens R environment settings placed at your home directory, 
on Linix it should be `~/.Renviron`
1. Add the following line with actual PAT value copied form github page:
```
GITHUB_PAT=<place here actual value of PAT>
```

Example of .Renviron file:
```
GITHUB_PAT=8c70fd8419398999c9ac5bacf3192882193cadf2
```

After that you may run build process and PAT would be automatically used
to retrieve required libraries from github. 
Don't concern about PAT privacy, it should not be included to the final distribution assembly.


More detailed instructions how to add GitHub PAT
can be found [here](https://happygitwithr.com/github-pat.html#step-by-step)