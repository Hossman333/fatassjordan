package jordansjones.core;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.yammer.dropwizard.client.JerseyClient;
import com.yammer.dropwizard.logging.Log;
import jordansjones.config.TweetUpdateServiceConfig;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class TweetUpdateService extends AbstractScheduledService {

	private static final Log logger = Log.forClass(TweetUpdateService.class);

	private final TweetUpdateServiceConfig config;
	private final JerseyClient httpClient;
	private final EventBus eventBus;
	private UriBuilder endpointUriBuilder;
	private Optional<Tweet> recentTweet = Optional.absent();

	@Inject
	public TweetUpdateService(final TweetUpdateServiceConfig config, final JerseyClient httpClient, final EventBus eventBus) {
		this.config = config;
		this.httpClient = httpClient;
		this.eventBus = eventBus;
		this.endpointUriBuilder = UriBuilder.fromPath("http://api.twitter.com/1/statuses/user_timeline.json")
			.queryParam("screen_name", config.getScreenName())
			.queryParam("trim_user", true);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void runOneIteration() throws Exception {
		logger.info("Update Tweets");
		Tweet tweet = recentTweet.get();
		if (tweet != null) {
			this.endpointUriBuilder = this.endpointUriBuilder
				.replaceQueryParam("since_id", tweet.getId());
		}

		final URI uri = this.endpointUriBuilder.build();
		final Tweet[] tweets = this.httpClient.get(uri, MediaType.APPLICATION_JSON_TYPE, Tweet[].class);
		if (tweets != null && tweets.length > 0)
			eventBus.post(tweets);

		if (tweets != null && tweets.length > 0) {
			if (tweet == null)
				tweet = tweets[0];

			for (Tweet t : tweets) {
				if (tweet.getCreatedAt().isBefore(t.getCreatedAt()))
					tweet = t;
			}
		}

		this.recentTweet = Optional.fromNullable(tweet);
	}

	@Override
	protected Scheduler scheduler() {
		final long initialDelay = config.getInitialDelay().toSeconds();
		final long period = config.getPeriod().toSeconds();
		return Scheduler.newFixedRateSchedule(initialDelay, period, TimeUnit.SECONDS);
	}
}