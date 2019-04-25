prevent_updates <- function(lib.loc = NULL, repos = getOption("repos"),
                            contriburl = contrib.url(repos, type),
                            method, instlib = NULL,
                            ask = TRUE, available = NULL,
                            oldPkgs = NULL, ..., checkBuilt = FALSE,
                            type = getOption("pkgType")) {
  
  cat(paste("update.packages called lib.loc =", if(is.null(lib.loc)) "NULL" else paste(lib.loc, collapse=',')), "\n")
  invisible(NULL)
}

prevent_devtools_updates <- function(pkgs = NULL, dependencies = NA,
                                     repos = getOption("repos"), type = getOption("pkgType")) {
  cat(paste("devtools::update_packages called pkgs =", if(is.null(pkgs)) "NULL" else paste(pkgs, collapse=',')), "\n")
  invisible(NULL)
}

.First <- function() {
  cat("Overriding utils::update.packages devtools::update_packages\n")
  library(utils)
  library(devtools)
  library(remotes)
  library(godmode)
  
  orig_install <- devtools::install
  
  orig_install_local <<- remotes::install_local
  
  remotes.install_local_wrapper <- function(path = ".", subdir = NULL,
                                            dependencies = NA,
                                            upgrade = c("default", "ask", "always", "never"),
                                            force = FALSE,
                                            quiet = FALSE,
                                            build = !is_binary_pkg(path),
                                            build_opts = c("--no-resave-data", "--no-manual", "--no-build-vignettes"),
                                            repos = getOption("repos"),
                                            type = getOption("pkgType"),
                                            ...) {
    cat(paste("remotes::install_local called path=", path, "\n"))
    if (is.function(orig_install_local)) {
      orig_install_local(path, subdir, dependencies, upgrade = "never", force, quiet, build, build_opts,
                          repos, type, ...)
    } else {
      warning("Original remotes::install_local is not a Function. Please, reinstall remotes package.")
    }
  }
  
#  godmode:::assignAnywhere("update.packages", prevent_updates)
#  godmode:::assignAnywhere("update_packages", prevent_devtools_updates)
#  godmode:::assignInName("remotes", "install_local", remotes.install_local_wrapper)
}
