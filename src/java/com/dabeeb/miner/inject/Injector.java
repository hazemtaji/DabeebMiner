package com.dabeeb.miner.inject;

import java.util.List;

public interface Injector {
	public List<InjectableURL> getUrls();
	public void reportInjected(List<InjectableURL> injectables);
	public void reportFailure(String url, InjectionStatus status);
	public void reportSuccess(String url);
}
