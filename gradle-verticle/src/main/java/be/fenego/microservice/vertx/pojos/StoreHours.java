package be.fenego.microservice.vertx.pojos;

public class StoreHours implements Comparable<StoreHours> {
	private String date;
	private String openingTime;
	private String closingTime;

	public StoreHours() {
	}

	public StoreHours(String date, String openingTime, String closingTime) {
		this();
		this.date = date;
		this.openingTime = openingTime;
		this.closingTime = closingTime;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getOpeningTime() {
		return openingTime;
	}

	public void setOpeningTime(String openingTime) {
		this.openingTime = openingTime;
	}

	public String getClosingTime() {
		return closingTime;
	}

	public void setClosingTime(String closingTime) {
		this.closingTime = closingTime;
	}

	@Override
	public int compareTo(StoreHours o) {
		return date.compareTo(o.getDate());
	}
}
