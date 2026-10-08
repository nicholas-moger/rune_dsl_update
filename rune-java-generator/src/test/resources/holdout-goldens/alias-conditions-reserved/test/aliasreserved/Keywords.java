package test.aliasreserved;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.aliasreserved.meta.KeywordsMeta;

import static java.util.Optional.ofNullable;

/**
 * Multi attributes whose Rune names are Java KEYWORDS - the wing&#39;s attribute-named local must be escaped for javac.
 * @version 0.0.0
 */
@RosettaDataType(value="Keywords", builder=Keywords.KeywordsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Keywords", model="test", builder=Keywords.KeywordsBuilderImpl.class, version="0.0.0")
public interface Keywords extends RosettaModelObject {

	KeywordsMeta metaData = new KeywordsMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getNew();
	List<Integer> getFinal();

	/*********************** Build Methods  ***********************/
	Keywords build();
	
	Keywords.KeywordsBuilder toBuilder();
	
	static Keywords.KeywordsBuilder builder() {
		return new Keywords.KeywordsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Keywords> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Keywords> getType() {
		return Keywords.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("new"), Integer.class, getNew(), this);
		processor.processBasic(path.newSubPath("final"), Integer.class, getFinal(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface KeywordsBuilder extends Keywords, RosettaModelObjectBuilder {
		Keywords.KeywordsBuilder addNew(Integer _new);
		Keywords.KeywordsBuilder addNew(Integer _new, int idx);
		Keywords.KeywordsBuilder addNew(List<Integer> _new);
		Keywords.KeywordsBuilder setNew(List<Integer> _new);
		Keywords.KeywordsBuilder addFinal(Integer _final);
		Keywords.KeywordsBuilder addFinal(Integer _final, int idx);
		Keywords.KeywordsBuilder addFinal(List<Integer> _final);
		Keywords.KeywordsBuilder setFinal(List<Integer> _final);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("new"), Integer.class, getNew(), this);
			processor.processBasic(path.newSubPath("final"), Integer.class, getFinal(), this);
		}
		

		Keywords.KeywordsBuilder prune();
	}

	/*********************** Immutable Implementation of Keywords  ***********************/
	class KeywordsImpl implements Keywords {
		private final List<Integer> _new;
		private final List<Integer> _final;
		
		protected KeywordsImpl(Keywords.KeywordsBuilder builder) {
			this._new = ofNullable(builder.getNew()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this._final = ofNullable(builder.getFinal()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("new")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("new")
		public List<Integer> getNew() {
			return _new;
		}
		
		@Override
		@RosettaAttribute("final")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("final")
		public List<Integer> getFinal() {
			return _final;
		}
		
		@Override
		public Keywords build() {
			return this;
		}
		
		@Override
		public Keywords.KeywordsBuilder toBuilder() {
			Keywords.KeywordsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Keywords.KeywordsBuilder builder) {
			ofNullable(getNew()).ifPresent(builder::setNew);
			ofNullable(getFinal()).ifPresent(builder::setFinal);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Keywords _that = getType().cast(o);
		
			if (!ListEquals.listEquals(_new, _that.getNew())) return false;
			if (!ListEquals.listEquals(_final, _that.getFinal())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (_new != null ? _new.hashCode() : 0);
			_result = 31 * _result + (_final != null ? _final.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Keywords {" +
				"new=" + this._new + ", " +
				"final=" + this._final +
			'}';
		}
	}

	/*********************** Builder Implementation of Keywords  ***********************/
	class KeywordsBuilderImpl implements Keywords.KeywordsBuilder {
	
		protected List<Integer> _new = new ArrayList<>();
		protected List<Integer> _final = new ArrayList<>();
		
		@Override
		@RosettaAttribute("new")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("new")
		public List<Integer> getNew() {
			return _new;
		}
		
		@Override
		@RosettaAttribute("final")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("final")
		public List<Integer> getFinal() {
			return _final;
		}
		
		@RosettaAttribute("new")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("new")
		@Override
		public Keywords.KeywordsBuilder addNew(Integer __new) {
			if (__new != null) {
				this._new.add(__new);
			}
			return this;
		}
		
		@Override
		public Keywords.KeywordsBuilder addNew(Integer __new, int idx) {
			getIndex(this._new, idx, () -> __new);
			return this;
		}
		
		@Override
		public Keywords.KeywordsBuilder addNew(List<Integer> news) {
			if (news != null) {
				for (final Integer toAdd : news) {
					this._new.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("new")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("new")
		@Override
		public Keywords.KeywordsBuilder setNew(List<Integer> news) {
			if (news == null) {
				this._new = new ArrayList<>();
			} else {
				this._new = news.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("final")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("final")
		@Override
		public Keywords.KeywordsBuilder addFinal(Integer __final) {
			if (__final != null) {
				this._final.add(__final);
			}
			return this;
		}
		
		@Override
		public Keywords.KeywordsBuilder addFinal(Integer __final, int idx) {
			getIndex(this._final, idx, () -> __final);
			return this;
		}
		
		@Override
		public Keywords.KeywordsBuilder addFinal(List<Integer> finals) {
			if (finals != null) {
				for (final Integer toAdd : finals) {
					this._final.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("final")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("final")
		@Override
		public Keywords.KeywordsBuilder setFinal(List<Integer> finals) {
			if (finals == null) {
				this._final = new ArrayList<>();
			} else {
				this._final = finals.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Keywords build() {
			return new Keywords.KeywordsImpl(this);
		}
		
		@Override
		public Keywords.KeywordsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Keywords.KeywordsBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getNew()!=null && !getNew().isEmpty()) return true;
			if (getFinal()!=null && !getFinal().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Keywords.KeywordsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Keywords.KeywordsBuilder o = (Keywords.KeywordsBuilder) other;
			
			
			merger.mergeBasic(getNew(), o.getNew(), (Consumer<Integer>) this::addNew);
			merger.mergeBasic(getFinal(), o.getFinal(), (Consumer<Integer>) this::addFinal);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Keywords _that = getType().cast(o);
		
			if (!ListEquals.listEquals(_new, _that.getNew())) return false;
			if (!ListEquals.listEquals(_final, _that.getFinal())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (_new != null ? _new.hashCode() : 0);
			_result = 31 * _result + (_final != null ? _final.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "KeywordsBuilder {" +
				"new=" + this._new + ", " +
				"final=" + this._final +
			'}';
		}
	}
}
