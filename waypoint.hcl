project = "prosanteconnect/pscload-v2"

# Labels can be specified for organizational purposes.
labels = {
  "domaine" = "psc"
}

runner {
  enabled = true
  data_source "git" {
    url = "https://github.com/prosanteconnect/pscload-v2.git"
    ref = var.datacenter
  }
  poll {
    enabled = true
  }
}

# An application to deploy.
app "prosanteconnect/pscload-v2" {
  # Build specifies how an application should be deployed. In this case,
  # we'll build using a Dockerfile and keeping it in a local registry.
  build {
    use "docker" {
      dockerfile = "${path.app}/${var.dockerfile_path}"
      disable_entrypoint = true
    }
    # Uncomment below to use a remote docker registry to push your built images.
    registry {
      use "docker" {
        image = "${var.registry_path}/pscload-v2"
        tag = gitrefpretty()
        encoded_auth = filebase64("/secrets/dockerAuth.json")
      }
    }
  }

  # Deploy to Nomad
  deploy {
    use "nomad-jobspec" {
      jobspec = templatefile("${path.app}/pscload-v2.nomad.tpl", {
        datacenter = var.datacenter
        proxy_port = var.proxy_port
        proxy_host = var.proxy_host
        non_proxy_hosts = var.non_proxy_hosts
        log_level = var.log_level
        registry_path = var.registry_path
      })
    }
  }
}

variable "datacenter" {
  type = string
  default = "dc1"
}

variable "registry_username" {
  type    = string
  default = ""
}

variable "registry_password" {
  type    = string
  default = ""
}

variable "proxy_port" {
  type = string
  default = ""
}

variable "proxy_host" {
  type = string
  default = ""
}

variable "non_proxy_hosts" {
  type = string
  default = "10.0.0.0/8"
}

variable "dockerfile_path" {
  type = string
  default = "Dockerfile"
}

variable "registry_path" {
  type = string
  default = "registry.repo.proxy-dev-forge.asip.hst.fluxus.net/prosanteconnect"
}

variable "log_level" {
  type = string
  default = "INFO"
}
