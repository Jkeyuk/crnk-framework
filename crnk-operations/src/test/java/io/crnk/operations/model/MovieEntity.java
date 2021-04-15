package io.crnk.operations.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import java.util.Set;
import java.util.UUID;

@Entity
public class MovieEntity extends MovieInfo {

	@Id
	private UUID id;

	@ManyToMany
	private Set<PersonEntity> directors;

	@OneToOne(cascade = CascadeType.ALL)
	private VoteEntity vote;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Set<PersonEntity> getDirectors() {
		return directors;
	}

	public void setDirectors(Set<PersonEntity> directors) {
		this.directors = directors;
	}

	public VoteEntity getVote() {
		return vote;
	}

	public void setVote(VoteEntity vote) {
		this.vote = vote;
	}
}
