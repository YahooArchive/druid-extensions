package com.yahoo.druid.emitter;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComposingEmitterConfig
{
  @JsonProperty
  @NotNull
  private List<String> emitters;

  public List<String> getEmitters() {
    return emitters;
  }
}
