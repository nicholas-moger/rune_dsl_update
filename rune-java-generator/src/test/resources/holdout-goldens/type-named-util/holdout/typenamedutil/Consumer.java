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
import holdout.typenamedutil.meta.ConsumerMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * java.util.function.Consumer - written by the POJO&#39;s getOrCreate arms over a model-typed attribute.
 * @version 0.0.0
 */
@RosettaDataType(value="Consumer", builder=Consumer.ConsumerBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Consumer", model="holdout", builder=Consumer.ConsumerBuilderImpl.class, version="0.0.0")
public interface Consumer extends RosettaModelObject {

	ConsumerMeta metaData = new ConsumerMeta();

	/*********************** Getter Methods  ***********************/
	List<String> getXs();
	String getX();
	Objects getInner();

	/*********************** Build Methods  ***********************/
	Consumer build();
	
	Consumer.ConsumerBuilder toBuilder();
	
	static Consumer.ConsumerBuilder builder() {
		return new Consumer.ConsumerBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Consumer> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Consumer> getType() {
		return Consumer.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		processRosetta(path.newSubPath("inner"), processor, Objects.class, getInner());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ConsumerBuilder extends Consumer, RosettaModelObjectBuilder {
		Objects.ObjectsBuilder getOrCreateInner();
		@Override
		Objects.ObjectsBuilder getInner();
		Consumer.ConsumerBuilder addXs(String xs);
		Consumer.ConsumerBuilder addXs(String xs, int idx);
		Consumer.ConsumerBuilder addXs(List<String> xs);
		Consumer.ConsumerBuilder setXs(List<String> xs);
		Consumer.ConsumerBuilder setX(String x);
		Consumer.ConsumerBuilder setInner(Objects inner);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("xs"), String.class, getXs(), this);
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
			processRosetta(path.newSubPath("inner"), processor, Objects.ObjectsBuilder.class, getInner());
		}
		

		Consumer.ConsumerBuilder prune();
	}

	/*********************** Immutable Implementation of Consumer  ***********************/
	class ConsumerImpl implements Consumer {
		private final List<String> xs;
		private final String x;
		private final Objects inner;
		
		protected ConsumerImpl(Consumer.ConsumerBuilder builder) {
			this.xs = ofNullable(builder.getXs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.x = builder.getX();
			this.inner = ofNullable(builder.getInner()).map(f->f.build()).orElse(null);
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
		@RosettaAttribute("inner")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("inner")
		public Objects getInner() {
			return inner;
		}
		
		@Override
		public Consumer build() {
			return this;
		}
		
		@Override
		public Consumer.ConsumerBuilder toBuilder() {
			Consumer.ConsumerBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Consumer.ConsumerBuilder builder) {
			ofNullable(getXs()).ifPresent(builder::setXs);
			ofNullable(getX()).ifPresent(builder::setX);
			ofNullable(getInner()).ifPresent(builder::setInner);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Consumer _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!java.util.Objects.equals(x, _that.getX())) return false;
			if (!java.util.Objects.equals(inner, _that.getInner())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			_result = 31 * _result + (inner != null ? inner.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Consumer {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x + ", " +
				"inner=" + this.inner +
			'}';
		}
	}

	/*********************** Builder Implementation of Consumer  ***********************/
	class ConsumerBuilderImpl implements Consumer.ConsumerBuilder {
	
		protected List<String> xs = new ArrayList<>();
		protected String x;
		protected Objects.ObjectsBuilder inner;
		
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
		@RosettaAttribute("inner")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("inner")
		public Objects.ObjectsBuilder getInner() {
			return inner;
		}
		
		@Override
		public Objects.ObjectsBuilder getOrCreateInner() {
			Objects.ObjectsBuilder result;
			if (inner!=null) {
				result = inner;
			}
			else {
				result = inner = Objects.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("xs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("xs")
		@Override
		public Consumer.ConsumerBuilder addXs(String _xs) {
			if (_xs != null) {
				this.xs.add(_xs);
			}
			return this;
		}
		
		@Override
		public Consumer.ConsumerBuilder addXs(String _xs, int idx) {
			getIndex(this.xs, idx, () -> _xs);
			return this;
		}
		
		@Override
		public Consumer.ConsumerBuilder addXs(List<String> xss) {
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
		public Consumer.ConsumerBuilder setXs(List<String> xss) {
			if (xss == null) {
				this.xs = new ArrayList<>();
			} else {
				this.xs = xss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("x")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("x")
		@Override
		public Consumer.ConsumerBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@RosettaAttribute("inner")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("inner")
		@Override
		public Consumer.ConsumerBuilder setInner(Objects _inner) {
			this.inner = _inner == null ? null : _inner.toBuilder();
			return this;
		}
		
		@Override
		public Consumer build() {
			return new Consumer.ConsumerImpl(this);
		}
		
		@Override
		public Consumer.ConsumerBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Consumer.ConsumerBuilder prune() {
			if (inner!=null && !inner.prune().hasData()) inner = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getXs()!=null && !getXs().isEmpty()) return true;
			if (getX()!=null) return true;
			if (getInner()!=null && getInner().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Consumer.ConsumerBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Consumer.ConsumerBuilder o = (Consumer.ConsumerBuilder) other;
			
			merger.mergeRosetta(getInner(), o.getInner(), this::setInner);
			
			merger.mergeBasic(getXs(), o.getXs(), (java.util.function.Consumer<String>) this::addXs);
			merger.mergeBasic(getX(), o.getX(), this::setX);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Consumer _that = getType().cast(o);
		
			if (!ListEquals.listEquals(xs, _that.getXs())) return false;
			if (!java.util.Objects.equals(x, _that.getX())) return false;
			if (!java.util.Objects.equals(inner, _that.getInner())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (xs != null ? xs.hashCode() : 0);
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			_result = 31 * _result + (inner != null ? inner.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ConsumerBuilder {" +
				"xs=" + this.xs + ", " +
				"x=" + this.x + ", " +
				"inner=" + this.inner +
			'}';
		}
	}
}
