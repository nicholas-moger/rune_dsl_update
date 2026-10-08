package test.twins.c;

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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.twins.c.meta.BothMeta;

import static java.util.Optional.ofNullable;

/**
 * Typed through two same-named aliases from two namespaces - the validator must wire two condition classes whose simple names collide (the import + the field name).
 * @version 0.0.0
 */
@RosettaDataType(value="Both", builder=Both.BothBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Both", model="test", builder=Both.BothBuilderImpl.class, version="0.0.0")
public interface Both extends RosettaModelObject {

	BothMeta metaData = new BothMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getLeft();
	Integer getRight();

	/*********************** Build Methods  ***********************/
	Both build();
	
	Both.BothBuilder toBuilder();
	
	static Both.BothBuilder builder() {
		return new Both.BothBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Both> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Both> getType() {
		return Both.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("left"), Integer.class, getLeft(), this);
		processor.processBasic(path.newSubPath("right"), Integer.class, getRight(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BothBuilder extends Both, RosettaModelObjectBuilder {
		Both.BothBuilder addLeft(Integer left);
		Both.BothBuilder addLeft(Integer left, int idx);
		Both.BothBuilder addLeft(List<Integer> left);
		Both.BothBuilder setLeft(List<Integer> left);
		Both.BothBuilder setRight(Integer right);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("left"), Integer.class, getLeft(), this);
			processor.processBasic(path.newSubPath("right"), Integer.class, getRight(), this);
		}
		

		Both.BothBuilder prune();
	}

	/*********************** Immutable Implementation of Both  ***********************/
	class BothImpl implements Both {
		private final List<Integer> left;
		private final Integer right;
		
		protected BothImpl(Both.BothBuilder builder) {
			this.left = ofNullable(builder.getLeft()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.right = builder.getRight();
		}
		
		@Override
		@RosettaAttribute("left")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("left")
		public List<Integer> getLeft() {
			return left;
		}
		
		@Override
		@RosettaAttribute("right")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("right")
		public Integer getRight() {
			return right;
		}
		
		@Override
		public Both build() {
			return this;
		}
		
		@Override
		public Both.BothBuilder toBuilder() {
			Both.BothBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Both.BothBuilder builder) {
			ofNullable(getLeft()).ifPresent(builder::setLeft);
			ofNullable(getRight()).ifPresent(builder::setRight);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Both _that = getType().cast(o);
		
			if (!ListEquals.listEquals(left, _that.getLeft())) return false;
			if (!Objects.equals(right, _that.getRight())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (left != null ? left.hashCode() : 0);
			_result = 31 * _result + (right != null ? right.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Both {" +
				"left=" + this.left + ", " +
				"right=" + this.right +
			'}';
		}
	}

	/*********************** Builder Implementation of Both  ***********************/
	class BothBuilderImpl implements Both.BothBuilder {
	
		protected List<Integer> left = new ArrayList<>();
		protected Integer right;
		
		@Override
		@RosettaAttribute("left")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("left")
		public List<Integer> getLeft() {
			return left;
		}
		
		@Override
		@RosettaAttribute("right")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("right")
		public Integer getRight() {
			return right;
		}
		
		@RosettaAttribute("left")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("left")
		@Override
		public Both.BothBuilder addLeft(Integer _left) {
			if (_left != null) {
				this.left.add(_left);
			}
			return this;
		}
		
		@Override
		public Both.BothBuilder addLeft(Integer _left, int idx) {
			getIndex(this.left, idx, () -> _left);
			return this;
		}
		
		@Override
		public Both.BothBuilder addLeft(List<Integer> lefts) {
			if (lefts != null) {
				for (final Integer toAdd : lefts) {
					this.left.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("left")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("left")
		@Override
		public Both.BothBuilder setLeft(List<Integer> lefts) {
			if (lefts == null) {
				this.left = new ArrayList<>();
			} else {
				this.left = lefts.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("right")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("right")
		@Override
		public Both.BothBuilder setRight(Integer _right) {
			this.right = _right == null ? null : _right;
			return this;
		}
		
		@Override
		public Both build() {
			return new Both.BothImpl(this);
		}
		
		@Override
		public Both.BothBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Both.BothBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getLeft()!=null && !getLeft().isEmpty()) return true;
			if (getRight()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Both.BothBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Both.BothBuilder o = (Both.BothBuilder) other;
			
			
			merger.mergeBasic(getLeft(), o.getLeft(), (Consumer<Integer>) this::addLeft);
			merger.mergeBasic(getRight(), o.getRight(), this::setRight);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Both _that = getType().cast(o);
		
			if (!ListEquals.listEquals(left, _that.getLeft())) return false;
			if (!Objects.equals(right, _that.getRight())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (left != null ? left.hashCode() : 0);
			_result = 31 * _result + (right != null ? right.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BothBuilder {" +
				"left=" + this.left + ", " +
				"right=" + this.right +
			'}';
		}
	}
}
