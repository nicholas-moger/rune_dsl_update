package holdout.typenamedutil;

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
import holdout.typenamedutil.meta.PatternMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * java.util.regex.Pattern - written by the type-format validator&#39;s string pattern check.
 * @version 0.0.0
 */
@RosettaDataType(value="Pattern", builder=Pattern.PatternBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Pattern", model="holdout", builder=Pattern.PatternBuilderImpl.class, version="0.0.0")
public interface Pattern extends RosettaModelObject {

	PatternMeta metaData = new PatternMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getP();

	/*********************** Build Methods  ***********************/
	Pattern build();
	
	Pattern.PatternBuilder toBuilder();
	
	static Pattern.PatternBuilder builder() {
		return new Pattern.PatternBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Pattern> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Pattern> getType() {
		return Pattern.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface PatternBuilder extends Pattern, RosettaModelObjectBuilder {
		Pattern.PatternBuilder addXs(String xs);
		Pattern.PatternBuilder addXs(String xs, int idx);
		Pattern.PatternBuilder addXs(List<String> xs);
		Pattern.PatternBuilder setXs(List<String> xs);
		Pattern.PatternBuilder setP(String p);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
		}
		

		Pattern.PatternBuilder prune();
	}

	/*********************** Immutable Implementation of Pattern  ***********************/
	class PatternImpl implements Pattern {
		private final List<String> xs;
		private final String p;
		
		protected PatternImpl(Pattern.PatternBuilder builder) {
			this.xs = ofNullable(builder.getXs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.p = builder.getP();
		}
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		public Pattern build() {
			return this;
		}
		
		@Override
		public Pattern.PatternBuilder toBuilder() {
			Pattern.PatternBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Pattern.PatternBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getP()).ifPresent(builder::setP);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Pattern _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!Objects.equals(p, _that.getP())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Pattern {" +
				"xs=" + this.xs + ", " +
				"p=" + this.p +
			'}';
		}
	}

	/*********************** Builder Implementation of Pattern  ***********************/
	class PatternBuilderImpl implements Pattern.PatternBuilder {
	
		protected List<String> xs = new ArrayList<>();
		protected String p;
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public Pattern.PatternBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public Pattern.PatternBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public Pattern.PatternBuilder addXs(List<String> xss) {
			if (xss != null) {
				for (final String toAdd : xss) {
					this.xs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public Pattern.PatternBuilder setXs(List<String> xss) {
			if (xss == null) {
				this.xs = new ArrayList<>();
			} else {
				this.xs = xss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("p")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("p")
		@Override
		public Pattern.PatternBuilder setP(String _p) {
			this.p = _p == null ? null : _p;
			return this;
		}
		
		@Override
		public Pattern build() {
			return new Pattern.PatternImpl(this);
		}
		
		@Override
		public Pattern.PatternBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Pattern.PatternBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getXs()!=null && !getXs().isEmpty()) return true;
			if (getP()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Pattern.PatternBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Pattern.PatternBuilder o = (Pattern.PatternBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getP(), o.getP(), this::setP);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Pattern _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!Objects.equals(p, _that.getP())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PatternBuilder {" +
				"xs=" + this.xs + ", " +
				"p=" + this.p +
			'}';
		}
	}
}
