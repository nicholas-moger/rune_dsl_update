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
import holdout.typenamedutil.meta.CollectorsMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

/**
 * java.util.stream.Collectors - written by the POJO&#39;s builder setters and the only-exists validator.
 * @version 0.0.0
 */
@RosettaDataType(value="Collectors", builder=Collectors.CollectorsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Collectors", model="holdout", builder=Collectors.CollectorsBuilderImpl.class, version="0.0.0")
public interface Collectors extends RosettaModelObject {

	CollectorsMeta metaData = new CollectorsMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();

	/*********************** Build Methods  ***********************/
	Collectors build();
	
	Collectors.CollectorsBuilder toBuilder();
	
	static Collectors.CollectorsBuilder builder() {
		return new Collectors.CollectorsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Collectors> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Collectors> getType() {
		return Collectors.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CollectorsBuilder extends Collectors, RosettaModelObjectBuilder {
		Collectors.CollectorsBuilder addXs(String xs);
		Collectors.CollectorsBuilder addXs(String xs, int idx);
		Collectors.CollectorsBuilder addXs(List<String> xs);
		Collectors.CollectorsBuilder setXs(List<String> xs);
		Collectors.CollectorsBuilder setX(String x);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		}
		

		Collectors.CollectorsBuilder prune();
	}

	/*********************** Immutable Implementation of Collectors  ***********************/
	class CollectorsImpl implements Collectors {
		private final List<String> xs;
		private final String x;
		
		protected CollectorsImpl(Collectors.CollectorsBuilder builder) {
			this.xs = ofNullable(builder.getXs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.x = builder.getX();
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
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@Override
		public Collectors build() {
			return this;
		}
		
		@Override
		public Collectors.CollectorsBuilder toBuilder() {
			Collectors.CollectorsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Collectors.CollectorsBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Collectors _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!Objects.equals(x, _that.getX())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Collectors {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}

	/*********************** Builder Implementation of Collectors  ***********************/
	class CollectorsBuilderImpl implements Collectors.CollectorsBuilder {
	
		protected List<String> xs = new ArrayList<>();
		protected String x;
		
		@Override
		@RosettaAttribute("xs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("xs")
		public List<String> getXs() {
			return xs;
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public Collectors.CollectorsBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public Collectors.CollectorsBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public Collectors.CollectorsBuilder addXs(List<String> xss) {
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
		public Collectors.CollectorsBuilder setXs(List<String> xss) {
			if (xss == null) {
				this.xs = new ArrayList<>();
			} else {
				this.xs = xss.stream()
					.collect(java.util.stream.Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("x")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("x")
		@Override
		public Collectors.CollectorsBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@Override
		public Collectors build() {
			return new Collectors.CollectorsImpl(this);
		}
		
		@Override
		public Collectors.CollectorsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Collectors.CollectorsBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getXs()!=null && !getXs().isEmpty()) return true;
			if (getX()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Collectors.CollectorsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Collectors.CollectorsBuilder o = (Collectors.CollectorsBuilder) other;
			
			
			merger.mergeBasic(getXs(), o.getXs(), (Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Collectors _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!Objects.equals(x, _that.getX())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CollectorsBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x +
			'}';
		}
	}
}
