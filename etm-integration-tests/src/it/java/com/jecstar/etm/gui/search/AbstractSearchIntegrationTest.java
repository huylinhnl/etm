package com.jecstar.etm.gui.search;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import com.jecstar.etm.gui.AbstractIntegrationTest;

public abstract class AbstractSearchIntegrationTest extends AbstractIntegrationTest {

	/**
	 * Wait for an event id to show up. This can be useful if an event is added,
	 * but still not present at the database. This method is filling in the
	 * search field and polling for the result to show up.
	 * 
	 * @param eventId
	 *            The event to search for,
	 * @return 
	 */
	protected WebElement waitForSearchResult(String eventId) {
		this.driver.findElement(By.id("query-string")).clear();
		this.driver.findElement(By.id("query-string")).sendKeys("id: " + eventId);
		this.driver.findElement(By.id("btn-search")).click();
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < 10000) {
			try {
				return this.driver.findElement(By.id(eventId));
			} catch (NoSuchElementException e) {
				try {
					Thread.sleep(750);
					this.driver.findElement(By.id("btn-search")).click();
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		}
		throw new NoSuchElementException("Element with id '" + eventId + "' not found.");
		
	}
}